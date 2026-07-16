package dev.linqfy.bigCasares.module;

import java.util.Objects;

public record ModuleLifecycleOutcome(String moduleId, ModuleLifecycleStatus status, Throwable failure) {

    public ModuleLifecycleOutcome {
        moduleId = Objects.requireNonNull(moduleId, "moduleId").trim();
        status = Objects.requireNonNull(status, "status");
        if (moduleId.isEmpty()) {
            throw new IllegalArgumentException("moduleId must not be blank");
        }
    }
}
