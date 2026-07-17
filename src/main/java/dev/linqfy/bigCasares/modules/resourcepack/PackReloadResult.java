package dev.linqfy.bigCasares.modules.resourcepack;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record PackReloadResult(
    UUID jobId,
    PackReloadStatus status,
    PackReloadPhase failurePhase,
    Duration duration,
    ActivePackManifest oldManifest,
    ActivePackManifest newManifest,
    List<String> warnings,
    Throwable failure
) {

    public PackReloadResult {
        jobId = Objects.requireNonNull(jobId, "jobId");
        status = Objects.requireNonNull(status, "status");
        duration = Objects.requireNonNull(duration, "duration");
        warnings = List.copyOf(warnings);
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Pack reload duration cannot be negative");
        }
        if (status == PackReloadStatus.FAILED && failurePhase == null) {
            throw new IllegalArgumentException("Failed pack reload requires a phase");
        }
        if (status != PackReloadStatus.FAILED && failurePhase != null) {
            throw new IllegalArgumentException("Only failed pack reloads have a failure phase");
        }
    }

    public boolean changed() {
        return status == PackReloadStatus.SUCCESS;
    }
}
