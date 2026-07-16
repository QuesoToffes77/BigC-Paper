package dev.linqfy.bigCasares.module.runtime;

import java.util.Objects;

public record RuntimeCleanupOutcome(String resourceId, Throwable failure) {

    public RuntimeCleanupOutcome {
        resourceId = Objects.requireNonNull(resourceId, "resourceId").trim();
        if (resourceId.isEmpty()) {
            throw new IllegalArgumentException("resourceId must not be blank");
        }
    }

    public boolean succeeded() {
        return failure == null;
    }
}
