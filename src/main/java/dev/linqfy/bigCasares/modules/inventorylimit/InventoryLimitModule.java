package dev.linqfy.bigCasares.modules.inventorylimit;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public final class InventoryLimitModule implements PluginModule {

    private final BigCasares plugin;

    private InventoryLimitService service;
    private InventoryLimitListener listener;
    private BukkitRuntimeRegistrations registrations;
    private RuntimeRegistrationScope runtimeScope;
    private RuntimeRegistrationScope compatibilityScope;

    public InventoryLimitModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "inventory-limit";
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        this.runtimeScope = scope;
        this.registrations = new BukkitRuntimeRegistrations(plugin, scope);
        reload();
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        listener = null;
        service = null;
        registrations = null;
        runtimeScope = null;
    }

    public BigCasares plugin() {
        return plugin;
    }

    public void reload() {
        this.service = new InventoryLimitService(new InventoryLimitSettingsLoader().load(plugin.getConfig()));
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            runtimeScope.forget("inventory-listener");
        }
        this.listener = new InventoryLimitListener(this, service);
        registrations.registerListener("inventory-listener", listener);
        
        runtimeScope.forget("inventory-enforcer-task");
        registrations.scheduleRepeating("inventory-enforcer-task", () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                enforce(player);
            }
        }, 2400L, 2400L);
    }

    public int enforce(Player player) {
        return service == null ? 0 : service.enforcePlayerInventory(player);
    }

    public void enforceLater(Player player) {
        BukkitRuntimeRegistrations current = registrations;
        if (current != null) {
            current.scheduleImmediate("inventory-enforcement", () -> enforce(player));
        }
    }
}
