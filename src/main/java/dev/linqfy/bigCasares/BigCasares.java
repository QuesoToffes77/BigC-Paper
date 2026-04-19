package dev.linqfy.bigCasares;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import dev.linqfy.bigCasares.module.ModuleManager;
import dev.linqfy.bigCasares.modules.copperapple.CopperAppleModule;
import dev.linqfy.bigCasares.modules.missions.MissionModule;
import org.bukkit.plugin.java.JavaPlugin;

public final class BigCasares extends JavaPlugin {

    private ModuleManager moduleManager;
    private CustomItemRegistry customItemRegistry;
    private MissionModule missionModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.customItemRegistry = new CustomItemRegistry();
        this.moduleManager = new ModuleManager(this, getConfig());

        this.missionModule = new MissionModule(this);

        moduleManager.register(new CopperAppleModule(this));
        moduleManager.register(missionModule);
        moduleManager.enableRegisteredModules();

        getLogger().info("BigCasares enabled. Active modules: "
            + moduleManager.getActiveModuleCount() + "/"
            + moduleManager.getRegisteredModuleCount());

    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableActiveModules();
        }
    }

    public CustomItemRegistry getCustomItemRegistry() {
        return customItemRegistry;
    }

    public MissionModule getMissionModule() {
        return missionModule;
    }
}
