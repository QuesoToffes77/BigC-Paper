package dev.linqfy.bigCasares.reload;

import dev.linqfy.bigCasares.module.ModuleLifecycleReport;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupReport;
import dev.linqfy.bigCasares.module.runtime.RuntimeGeneration;

import java.util.List;

public interface ReloadOperation {

    default boolean isNoOp() {
        return false;
    }

    ModuleLifecycleReport disableRuntime();

    RuntimeCleanupReport cleanupRuntime();

    void reloadConfiguration();

    ModuleLifecycleReport initializeRuntime(RuntimeGeneration generation);

    void bindCommands();

    int activeModuleCount();

    int registeredModuleCount();

    default List<String> warnings() {
        return List.of();
    }
}
