package dev.linqfy.bigCasares;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import dev.linqfy.bigCasares.command.BigCasaresCommand;
import dev.linqfy.bigCasares.module.ModuleManager;
import dev.linqfy.bigCasares.modules.bounties.BountyModule;
import dev.linqfy.bigCasares.modules.copperapple.CopperAppleModule;
import dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitModule;
import dev.linqfy.bigCasares.modules.missions.MissionModule;
import dev.linqfy.bigCasares.modules.shop.ShopModule;
import dev.linqfy.bigCasares.modules.skillrating.SkillRatingModule;
import dev.linqfy.bigCasares.modules.smokebomb.SmokeBombModule;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class BigCasares extends JavaPlugin {

    private ModuleManager moduleManager;
    private CustomItemRegistry customItemRegistry;
    private MissionModule missionModule;
    private BountyModule bountyModule;
    private ShopModule shopModule;
    private InventoryLimitModule inventoryLimitModule;
    private SkillRatingModule skillRatingModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeRuntime();
        registerCommands();

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

    public BountyModule getBountyModule() {
        return bountyModule;
    }

    public ShopModule getShopModule() {
        return shopModule;
    }

    public InventoryLimitModule getInventoryLimitModule() {
        return inventoryLimitModule;
    }

    public SkillRatingModule getSkillRatingModule() {
        return skillRatingModule;
    }

    public void reloadPluginState() {
        if (moduleManager != null) {
            moduleManager.disableActiveModules();
        }
        reloadConfig();
        initializeRuntime();
        registerCommands();
    }

    public void openShop(Player player) {
        if (shopModule == null || !shopModule.isEnabled()) {
            player.sendMessage("§cEl shop no esta disponible.");
            return;
        }
        shopModule.openMainMenu(player);
    }

    public int enforceInventoryLimits(Player player) {
        return inventoryLimitModule == null ? 0 : inventoryLimitModule.enforce(player);
    }

    private void initializeRuntime() {
        this.customItemRegistry = new CustomItemRegistry();
        this.moduleManager = new ModuleManager(this, getConfig());
        this.missionModule = new MissionModule(this);
        this.bountyModule = new BountyModule(this);
        this.shopModule = new ShopModule(this);
        this.inventoryLimitModule = new InventoryLimitModule(this);
        this.skillRatingModule = new SkillRatingModule(this);

        moduleManager.register(new CopperAppleModule(this));
        moduleManager.register(new SmokeBombModule(this));
        moduleManager.register(missionModule);
        moduleManager.register(bountyModule);
        moduleManager.register(shopModule);
        moduleManager.register(inventoryLimitModule);
        moduleManager.register(skillRatingModule);
        moduleManager.enableRegisteredModules();
    }

    private void registerCommands() {
        BigCasaresCommand handler = new BigCasaresCommand(this);
        PluginCommand management = getCommand("bigcasares");
        if (management != null) {
            management.setExecutor(handler);
            management.setTabCompleter(handler);
        }
        PluginCommand shop = getCommand("shop");
        if (shop != null) {
            shop.setExecutor(handler);
            shop.setTabCompleter(handler);
        }
    }
}
