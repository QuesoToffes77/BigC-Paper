package dev.linqfy.bigCasares.modules.shop;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.modules.bounties.BountyEconomyGateway;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public final class ShopModule implements PluginModule {

    private final BigCasares plugin;

    private ShopCatalog catalog = new ShopCatalog(java.util.List.of());
    private ShopService service;
    private ShopGuiController guiController;
    private boolean enabled;

    public ShopModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "shop-system";
    }

    @Override
    public void onEnable() {
        plugin.saveResource("shop.yml", false);
        reload();
    }

    @Override
    public void onDisable() {
        enabled = false;
        if (guiController != null) {
            guiController.closeAll();
            org.bukkit.event.HandlerList.unregisterAll(guiController);
        }
    }

    public BigCasares plugin() {
        return plugin;
    }

    public void reload() {
        Economy economy = BountyEconomyGateway.resolveOrThrow(plugin);
        File file = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        this.catalog = new ShopCatalogLoader().load(configuration);
        this.service = new ShopService(
            new ShopItemResolver(plugin.getCustomItemRegistry()),
            new ShopEconomyGateway(plugin, economy),
            plugin::enforceInventoryLimits
        );
        if (guiController != null) {
            guiController.closeAll();
            org.bukkit.event.HandlerList.unregisterAll(guiController);
        }
        this.guiController = new ShopGuiController(this, catalog, service);
        plugin.getServer().getPluginManager().registerEvents(guiController, plugin);
        this.enabled = true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void openMainMenu(Player player) {
        if (!enabled || guiController == null) {
            player.sendMessage("§cEl shop no esta disponible.");
            return;
        }
        guiController.openCategories(player);
    }
}
