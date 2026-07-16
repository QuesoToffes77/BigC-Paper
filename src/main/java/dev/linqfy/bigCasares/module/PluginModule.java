package dev.linqfy.bigCasares.module;

import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;

public interface PluginModule {

    String getId();

    void onEnable();

    default void onEnable(RuntimeRegistrationScope scope) {
        onEnable();
    }

    void onDisable();
}
