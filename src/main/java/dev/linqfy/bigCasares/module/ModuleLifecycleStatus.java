package dev.linqfy.bigCasares.module;

public enum ModuleLifecycleStatus {
    ENABLED,
    DISABLED_BY_CONFIG,
    ENABLE_FAILED,
    DISABLED,
    DISABLE_FAILED,
    CLEANUP_FAILED;

    public boolean isFailure() {
        return this == ENABLE_FAILED || this == DISABLE_FAILED || this == CLEANUP_FAILED;
    }
}
