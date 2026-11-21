package dev.linqfy.bigCasares.modules.staffevents;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.dimension.DimensionPlatform;
import dev.linqfy.bigCasares.dimension.DimensionStatus;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.InventoryCustody;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.Predicate;

/**
 * {@code staff-events}: events only staff can start (Lifesteal, KOTH, TNT Tag, Spleef).
 * Registers the void event dimension through the dimension framework, so it must be
 * registered after {@code dimension-framework}.
 */
public final class StaffEventsModule implements PluginModule {

    public static final String MODULE_ID = "staff-events";

    private final BigCasares plugin;
    private final Supplier<Optional<DimensionPlatform>> dimensions;
    private final Supplier<Optional<Boolean>> pvpBaseline;
    private final Predicate<UUID> endHuntDeath;
    private RuntimeRegistrationScope compatibilityScope;
    private StaffEventRuntime runtime;

    public StaffEventsModule(
        BigCasares plugin,
        Supplier<Optional<DimensionPlatform>> dimensions,
        Supplier<Optional<Boolean>> pvpBaseline,
        Predicate<UUID> endHuntDeath
    ) {
        this.plugin = plugin;
        this.dimensions = dimensions;
        this.pvpBaseline = pvpBaseline;
        this.endHuntDeath = endHuntDeath;
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        StaffEventSettings settings = StaffEventSettings.load(plugin.getConfig());
        settings.warnings().forEach(warning -> plugin.getLogger().warning("[Eventos] " + warning));
        EventSnapshotStore store = new EventSnapshotStore(
            plugin.getDataFolder().toPath().resolve("data/staff-events/snapshots.yml"));
        try {
            store.load();
        } catch (IOException failure) {
            // Refuse to run rather than overwrite inventories that are still waiting to be returned.
            throw new IllegalStateException("Could not read staff event inventories", failure);
        }
        if (settings.arenaDimension()) {
            registerDimension(scope);
        }

        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        StaffEventRuntime created = new StaffEventRuntime(
            plugin, settings, store, dimensions, this::rewardItem, pvpBaseline, endHuntDeath, registrations);
        InventoryCustody custody = created.custody();
        var services = plugin.getServer().getServicesManager();
        try {
            services.register(InventoryCustody.class, custody, plugin, ServicePriority.Normal);
            scope.register("staff-events-custody", () -> services.unregister(InventoryCustody.class, custody));
        } catch (RuntimeException | Error failure) {
            try {
                services.unregister(InventoryCustody.class, custody);
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        scope.register("staff-events-runtime", () -> {
            created.shutdown();
            runtime = null;
        });
        runtime = created;
        registrations.registerListener("staff-events-listener", created);
        PluginCommand command = plugin.getCommand("evento");
        if (command == null) {
            throw new IllegalStateException("Required command is not declared: evento");
        }
        StaffEventCommand handler = new StaffEventCommand(created);
        registrations.bindCommand("staff-events-command", command, handler, handler);
        registrations.scheduleRepeating("staff-events-tick", created::tick, 5L, 5L);
        registrations.scheduleRepeating("staff-events-prepare", created::prepareTick, 1L, 1L);
        registrations.scheduleRepeating("staff-events-recover", created::retryBlockedRestores, 100L, 100L);
        // Other inventory owners register during module startup; recover only after they are visible.
        registrations.scheduleImmediate("staff-events-initial-restore", created::restorePendingOnline);
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
    }

    /** Whether a death is already handled by a running event (keep inventory, no tombstone). */
    public boolean protectsDeath(UUID playerId) {
        StaffEventRuntime current = runtime;
        return current != null && current.protectsDeath(playerId);
    }

    private void registerDimension(RuntimeRegistrationScope scope) {
        Optional<DimensionPlatform> platform = dimensions.get();
        if (platform.isEmpty()) {
            plugin.getLogger().warning("[Eventos] dimension-framework is disabled; arenas need fallback-arena-world.");
            return;
        }
        try {
            DimensionStatus status = platform.get().register(EventArenaDimension.create());
            plugin.getLogger().info("[Eventos] Registered " + EventArenaDimension.KEY + ": " + status.state()
                + (status.message().isBlank() ? "" : " (" + status.message() + ")"));
            scope.register("staff-events-dimension", () -> platform.get().unregister(EventArenaDimension.KEY));
        } catch (RuntimeException failure) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "[Eventos] Could not register the event dimension", failure);
        }
    }

    private Optional<ItemStack> rewardItem(RewardItem reward) {
        if (reward.vanilla()) {
            Material material = Material.matchMaterial(reward.id());
            return material == null || !material.isItem()
                ? Optional.empty()
                : Optional.of(new ItemStack(material, reward.amount()));
        }
        var registry = plugin.getCustomItemRegistry();
        return registry == null
            ? Optional.empty()
            : registry.findById(reward.id()).map(item -> item.createItemStack(reward.amount()));
    }
}
