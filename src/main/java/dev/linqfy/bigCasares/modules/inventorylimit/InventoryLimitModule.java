package dev.linqfy.bigCasares.modules.inventorylimit;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import org.bukkit.entity.Player;

public final class InventoryLimitModule implements PluginModule {

    private final BigCasares plugin;

    private InventoryLimitService service;
    private InventoryLimitListener listener;

    public InventoryLimitModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "inventory-limit";
    }

    @Override
    public void onEnable() {
        reload();
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
    }

    public BigCasares plugin() {
        return plugin;
    }

    public void reload() {
        this.service = new InventoryLimitService(new InventoryLimitSettingsLoader().load(plugin.getConfig()));
        if (listener != null) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
        this.listener = new InventoryLimitListener(this, service);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    public int enforce(Player player) {
        return service == null ? 0 : service.enforcePlayerInventory(player);
    }

    public void enforceLater(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> enforce(player));
    }
}
