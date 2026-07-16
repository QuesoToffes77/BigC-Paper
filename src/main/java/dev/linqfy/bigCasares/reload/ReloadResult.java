package dev.linqfy.bigCasares.reload;

import dev.linqfy.bigCasares.module.ModuleLifecycleOutcome;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupOutcome;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record ReloadResult(
    ReloadStatus status,
    Duration duration,
    long oldGenerationId,
    long newGenerationId,
    ReloadPhase failurePhase,
    List<ModuleLifecycleOutcome> moduleOutcomes,
    List<RuntimeCleanupOutcome> cleanupOutcomes,
    List<String> warnings,
    Throwable failure,
    int activeModules,
    int registeredModules
) {

    public static final long UNKNOWN_GENERATION = -1;

    public ReloadResult {
        status = Objects.requireNonNull(status, "status");
        duration = Objects.requireNonNull(duration, "duration");
        moduleOutcomes = List.copyOf(moduleOutcomes);
        cleanupOutcomes = List.copyOf(cleanupOutcomes);
        warnings = List.copyOf(warnings);
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Reload duration must not be negative");
        }
        if (activeModules < 0 || registeredModules < 0 || activeModules > registeredModules) {
            throw new IllegalArgumentException("Invalid module counts");
        }
        if (status == ReloadStatus.FAILED && failurePhase == null) {
            throw new IllegalArgumentException("Failed reload requires a failure phase");
        }
        if (status != ReloadStatus.FAILED && failurePhase != null) {
            throw new IllegalArgumentException("Only failed reloads have a failure phase");
        }
    }

    public static ReloadResult success(
        List<ModuleLifecycleOutcome> moduleOutcomes,
        List<RuntimeCleanupOutcome> cleanupOutcomes,
        int activeModules,
        int registeredModules
    ) {
        return success(Duration.ZERO, UNKNOWN_GENERATION, UNKNOWN_GENERATION,
            moduleOutcomes, cleanupOutcomes, List.of(), activeModules, registeredModules);
    }

    public static ReloadResult success(
        Duration duration,
        long oldGenerationId,
        long newGenerationId,
        List<ModuleLifecycleOutcome> moduleOutcomes,
        List<RuntimeCleanupOutcome> cleanupOutcomes,
        List<String> warnings,
        int activeModules,
        int registeredModules
    ) {
        return new ReloadResult(ReloadStatus.SUCCESS, duration, oldGenerationId, newGenerationId, null,
            moduleOutcomes, cleanupOutcomes, warnings, null, activeModules, registeredModules);
    }

    public static ReloadResult noOp(
        Duration duration,
        long generationId,
        List<String> warnings,
        int activeModules,
        int registeredModules
    ) {
        return new ReloadResult(ReloadStatus.NO_OP, duration, generationId, generationId, null,
            List.of(), List.of(), warnings, null, activeModules, registeredModules);
    }

    public static ReloadResult alreadyRunning() {
        return alreadyRunning(UNKNOWN_GENERATION);
    }

    public static ReloadResult alreadyRunning(long generationId) {
        return new ReloadResult(ReloadStatus.ALREADY_RUNNING, Duration.ZERO,
            generationId, generationId, null, List.of(), List.of(), List.of(), null, 0, 0);
    }

    public static ReloadResult failed(
        ReloadPhase phase,
        List<ModuleLifecycleOutcome> moduleOutcomes,
        List<RuntimeCleanupOutcome> cleanupOutcomes,
        Throwable failure,
        int activeModules,
        int registeredModules
    ) {
        return failed(Duration.ZERO, UNKNOWN_GENERATION, UNKNOWN_GENERATION, phase,
            moduleOutcomes, cleanupOutcomes, List.of(), failure, activeModules, registeredModules);
    }

    public static ReloadResult failed(
        Duration duration,
        long oldGenerationId,
        long newGenerationId,
        ReloadPhase phase,
        List<ModuleLifecycleOutcome> moduleOutcomes,
        List<RuntimeCleanupOutcome> cleanupOutcomes,
        List<String> warnings,
        Throwable failure,
        int activeModules,
        int registeredModules
    ) {
        return new ReloadResult(ReloadStatus.FAILED, duration, oldGenerationId, newGenerationId, phase,
            moduleOutcomes, cleanupOutcomes, warnings, failure, activeModules, registeredModules);
    }

    public List<RuntimeCleanupOutcome> cleanupFailures() {
        return cleanupOutcomes.stream()
            .filter(outcome -> !outcome.succeeded())
            .toList();
    }
}
