package dev.linqfy.bigCasares.reload;

import dev.linqfy.bigCasares.module.ModuleLifecycleOutcome;
import dev.linqfy.bigCasares.module.ModuleLifecycleReport;
import dev.linqfy.bigCasares.module.ModuleLifecycleStatus;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupOutcome;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupReport;
import dev.linqfy.bigCasares.module.runtime.RuntimeGeneration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class ReloadCoordinator {

    private final AtomicBoolean reloading = new AtomicBoolean();
    private final AtomicLong currentGenerationId;
    private final AtomicLong generationReservation;
    private final LongSupplier nanoTime;

    public ReloadCoordinator() {
        this(0);
    }

    public ReloadCoordinator(long initialGenerationId) {
        this(initialGenerationId, System::nanoTime);
    }

    ReloadCoordinator(long initialGenerationId, LongSupplier nanoTime) {
        if (initialGenerationId < 0) {
            throw new IllegalArgumentException("Initial generation id must be non-negative");
        }
        this.currentGenerationId = new AtomicLong(initialGenerationId);
        this.generationReservation = new AtomicLong(initialGenerationId);
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    public ReloadResult execute(ReloadOperation operation) {
        Objects.requireNonNull(operation, "operation");
        if (!reloading.compareAndSet(false, true)) {
            return ReloadResult.alreadyRunning(currentGenerationId.get());
        }

        long started = nanoTime.getAsLong();
        long oldGenerationId = currentGenerationId.get();
        List<ModuleLifecycleOutcome> moduleOutcomes = new ArrayList<>();
        List<RuntimeCleanupOutcome> cleanupOutcomes = new ArrayList<>();
        try {
            try {
                if (operation.isNoOp()) {
                    return ReloadResult.noOp(durationSince(started), oldGenerationId, safeWarnings(operation),
                        safeCount(operation::activeModuleCount), safeCount(operation::registeredModuleCount));
                }
            } catch (Throwable failure) {
                return failed(operation, started, oldGenerationId, ReloadResult.UNKNOWN_GENERATION,
                    ReloadPhase.PREPARATION, moduleOutcomes, cleanupOutcomes, failure);
            }

            ModuleLifecycleReport shutdown = disable(operation, moduleOutcomes);
            RuntimeCleanupReport cleanup = cleanup(operation, cleanupOutcomes);
            if (shutdown.hasFailures()) {
                return failed(operation, started, oldGenerationId, ReloadResult.UNKNOWN_GENERATION,
                    ReloadPhase.SHUTDOWN, moduleOutcomes, cleanupOutcomes,
                    shutdown.failures().getFirst().failure());
            }
            if (cleanup.hasFailures()) {
                return failed(operation, started, oldGenerationId, ReloadResult.UNKNOWN_GENERATION,
                    ReloadPhase.CLEANUP, moduleOutcomes, cleanupOutcomes,
                    cleanup.failures().getFirst().failure());
            }

            try {
                operation.reloadConfiguration();
            } catch (Throwable failure) {
                return failed(operation, started, oldGenerationId, ReloadResult.UNKNOWN_GENERATION,
                    ReloadPhase.CONFIGURATION, moduleOutcomes, cleanupOutcomes, failure);
            }

            long candidateGenerationId = generationReservation.incrementAndGet();
            RuntimeGeneration candidateGeneration = new RuntimeGeneration(candidateGenerationId);
            ModuleLifecycleReport activation;
            try {
                activation = operation.initializeRuntime(candidateGeneration);
                moduleOutcomes.addAll(activation.outcomes());
            } catch (Throwable failure) {
                cleanupCandidate(operation, candidateGeneration, moduleOutcomes, cleanupOutcomes);
                return failed(operation, started, oldGenerationId, candidateGenerationId,
                    ReloadPhase.CANDIDATE_ENABLE, moduleOutcomes, cleanupOutcomes, failure);
            }
            if (activation.hasFailures()) {
                cleanupCandidate(operation, candidateGeneration, moduleOutcomes, cleanupOutcomes);
                return failed(operation, started, oldGenerationId, candidateGenerationId,
                    ReloadPhase.CANDIDATE_ENABLE, moduleOutcomes, cleanupOutcomes,
                    activation.failures().getFirst().failure());
            }

            try {
                operation.bindCommands();
            } catch (Throwable failure) {
                cleanupCandidate(operation, candidateGeneration, moduleOutcomes, cleanupOutcomes);
                return failed(operation, started, oldGenerationId, candidateGenerationId,
                    ReloadPhase.COMMAND_BINDING, moduleOutcomes, cleanupOutcomes, failure);
            }

            currentGenerationId.set(candidateGenerationId);
            return ReloadResult.success(durationSince(started), oldGenerationId, candidateGenerationId,
                moduleOutcomes, cleanupOutcomes, safeWarnings(operation),
                safeCount(operation::activeModuleCount), safeCount(operation::registeredModuleCount));
        } catch (Throwable failure) {
            return failed(operation, started, oldGenerationId, ReloadResult.UNKNOWN_GENERATION,
                ReloadPhase.INTERNAL, moduleOutcomes, cleanupOutcomes, failure);
        } finally {
            reloading.set(false);
        }
    }

    public boolean isReloading() {
        return reloading.get();
    }

    public long currentGenerationId() {
        return currentGenerationId.get();
    }

    private void cleanupCandidate(
        ReloadOperation operation,
        RuntimeGeneration candidateGeneration,
        List<ModuleLifecycleOutcome> moduleOutcomes,
        List<RuntimeCleanupOutcome> cleanupOutcomes
    ) {
        candidateGeneration.retire();
        disable(operation, moduleOutcomes);
        cleanup(operation, cleanupOutcomes);
    }

    private ModuleLifecycleReport disable(
        ReloadOperation operation,
        List<ModuleLifecycleOutcome> aggregate
    ) {
        try {
            ModuleLifecycleReport report = operation.disableRuntime();
            aggregate.addAll(report.outcomes());
            return report;
        } catch (Throwable failure) {
            ModuleLifecycleReport report = new ModuleLifecycleReport(List.of(new ModuleLifecycleOutcome(
                "runtime",
                ModuleLifecycleStatus.DISABLE_FAILED,
                failure
            )));
            aggregate.addAll(report.outcomes());
            return report;
        }
    }

    private RuntimeCleanupReport cleanup(
        ReloadOperation operation,
        List<RuntimeCleanupOutcome> aggregate
    ) {
        try {
            RuntimeCleanupReport report = operation.cleanupRuntime();
            aggregate.addAll(report.outcomes());
            return report;
        } catch (Throwable failure) {
            RuntimeCleanupReport report = new RuntimeCleanupReport(List.of(new RuntimeCleanupOutcome(
                "runtime-cleanup",
                failure
            )), false);
            aggregate.addAll(report.outcomes());
            return report;
        }
    }

    private ReloadResult failed(
        ReloadOperation operation,
        long started,
        long oldGenerationId,
        long newGenerationId,
        ReloadPhase phase,
        List<ModuleLifecycleOutcome> moduleOutcomes,
        List<RuntimeCleanupOutcome> cleanupOutcomes,
        Throwable failure
    ) {
        return ReloadResult.failed(durationSince(started), oldGenerationId, newGenerationId, phase,
            moduleOutcomes, cleanupOutcomes, safeWarnings(operation), failure,
            safeCount(operation::activeModuleCount), safeCount(operation::registeredModuleCount));
    }

    private List<String> safeWarnings(ReloadOperation operation) {
        try {
            List<String> warnings = operation.warnings();
            return warnings == null ? List.of("No se pudieron recopilar todas las advertencias.") : warnings;
        } catch (Throwable ignored) {
            return List.of("No se pudieron recopilar todas las advertencias.");
        }
    }

    private int safeCount(CountSupplier supplier) {
        try {
            return Math.max(0, supplier.get());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private Duration durationSince(long started) {
        return Duration.ofNanos(Math.max(0, nanoTime.getAsLong() - started));
    }

    @FunctionalInterface
    private interface CountSupplier {
        int get();
    }
}
