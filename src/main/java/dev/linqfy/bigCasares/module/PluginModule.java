package dev.linqfy.bigCasares.module;

public interface PluginModule {

    String getId();

    void onEnable();

    void onDisable();
}
