package dev.linqfy.bigCasares.modules.resourcepack;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public final class PackReloadCoordinator {

    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicReference<UUID> currentJob = new AtomicReference<>();
    private final LongSupplier nanoTime;

    public PackReloadCoordinator() {
        this(System::nanoTime);
    }

    PackReloadCoordinator(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    public CompletableFuture<PackReloadResult> execute(
        PackReloadOperation operation,
        Executor prepareExecutor,
        Executor commitExecutor
    ) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(prepareExecutor, "prepareExecutor");
        Objects.requireNonNull(commitExecutor, "commitExecutor");
        UUID jobId = UUID.randomUUID();
        if (!active.get()) {
            return CompletableFuture.completedFuture(cancelled(jobId, null, null, Duration.ZERO));
        }
        if (!running.compareAndSet(false, true)) {
            UUID activeJob = currentJob.get();
            return CompletableFuture.completedFuture(new PackReloadResult(
                activeJob == null ? new UUID(0L, 0L) : activeJob,
                PackReloadStatus.ALREADY_RUNNING, null, Duration.ZERO,
                null, null, List.of(), null));
        }
        currentJob.set(jobId);
        long started = nanoTime.getAsLong();
        ActivePackManifest oldManifest;
        try {
            oldManifest = operation.activeManifest().orElse(null);
        } catch (Throwable failure) {
            running.set(false);
            currentJob.set(null);
            return CompletableFuture.completedFuture(failed(
                jobId, started, null, PackReloadPhase.DISCOVERY, failure));
        }

        CompletableFuture<Prepared> prepared = CompletableFuture.supplyAsync(() -> {
            requireActive();
            try {
                return new Prepared(operation.prepare(jobId));
            } catch (Throwable failure) {
                if (failure instanceof PackReloadException packFailure) {
                    throw new PhaseFailure(packFailure.phase(), packFailure.getCause());
                }
                throw new PhaseFailure(PackReloadPhase.BUILD, failure);
            }
        }, prepareExecutor);

        CompletableFuture<PackReloadResult> result = prepared.thenApplyAsync(candidate -> {
            requireActive();
            PackPublication publication = candidate.publication();
            if (!publication.changed()) {
                return new PackReloadResult(
                    jobId, PackReloadStatus.NO_OP, null, durationSince(started),
                    oldManifest, publication.manifest(), List.of(), null);
            }
            try {
                operation.commit(publication);
            } catch (Throwable failure) {
                throw new PhaseFailure(PackReloadPhase.COMMIT, failure);
            }
            return new PackReloadResult(
                jobId, PackReloadStatus.SUCCESS, null, durationSince(started),
                oldManifest, publication.manifest(), List.of(), null);
        }, commitExecutor).exceptionally(failure -> {
            Throwable cause = unwrap(failure);
            if (cause instanceof RetiredCoordinatorException) {
                return cancelled(jobId, oldManifest, null, durationSince(started));
            }
            if (cause instanceof PhaseFailure phaseFailure) {
                return failed(jobId, started, oldManifest, phaseFailure.phase(), phaseFailure.getCause());
            }
            return failed(jobId, started, oldManifest, PackReloadPhase.INTERNAL, cause);
        });
        return result.whenComplete((ignored, failure) -> {
            currentJob.compareAndSet(jobId, null);
            running.set(false);
        });
    }

    public void retire() {
        active.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }

    public Optional<UUID> currentJobId() {
        return Optional.ofNullable(currentJob.get());
    }

    private void requireActive() {
        if (!active.get()) {
            throw new RetiredCoordinatorException();
        }
    }

    private PackReloadResult failed(
        UUID jobId,
        long started,
        ActivePackManifest oldManifest,
        PackReloadPhase phase,
        Throwable failure
    ) {
        return new PackReloadResult(
            jobId, PackReloadStatus.FAILED, phase, durationSince(started),
            oldManifest, null, List.of(), failure);
    }

    private static PackReloadResult cancelled(
        UUID jobId,
        ActivePackManifest oldManifest,
        ActivePackManifest newManifest,
        Duration duration
    ) {
        return new PackReloadResult(
            jobId, PackReloadStatus.CANCELLED, null, duration,
            oldManifest, newManifest, List.of(), null);
    }

    private Duration durationSince(long started) {
        return Duration.ofNanos(Math.max(0L, nanoTime.getAsLong() - started));
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException || current instanceof java.util.concurrent.ExecutionException)
            && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private record Prepared(PackPublication publication) {
    }

    private static final class PhaseFailure extends RuntimeException {
        private final PackReloadPhase phase;

        private PhaseFailure(PackReloadPhase phase, Throwable cause) {
            super(cause);
            this.phase = phase;
        }

        private PackReloadPhase phase() {
            return phase;
        }
    }

    private static final class RetiredCoordinatorException extends RuntimeException {
    }
}
