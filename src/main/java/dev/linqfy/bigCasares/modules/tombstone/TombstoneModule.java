package dev.linqfy.bigCasares.modules.tombstone;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class TombstoneModule implements PluginModule {
    private final BigCasares plugin;
    private TombstoneRuntime runtime;
    private RuntimeRegistrationScope compatibilityScope;
    private Predicate<UUID> deathCaptureEligibility = ignored -> true;

    public TombstoneModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "death-tombstone-system"; }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        long lifetimeSeconds = Math.max(1L, plugin.getConfig().getLong("death-tombstone-system.lifetime-seconds", 900L));
        TombstoneRuntime created = new TombstoneRuntime(
            plugin,
            new YamlTombstoneStorage(plugin.getDataFolder().toPath().resolve("data/tombstones/tombstones.yml")),
            Clock.systemUTC(), Duration.ofSeconds(lifetimeSeconds), registrations
        );
        registrations.registerListener("tombstone-listener",
            new TombstoneListener(created, registrations, this::shouldCaptureDeath));
        // Registered before load(): a failed load still removes whatever visuals it already spawned.
        scope.register("tombstone-runtime", () -> {
            if (runtime == created) runtime = null;
            created.shutdown();
        });
        created.load();
        plugin.getServer().getOnlinePlayers().forEach(created::recoverBurial);
        this.runtime = created;
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) scope.close();
    }

    public boolean openNearby(Player player, double radius) {
        TombstoneRuntime current = runtime;
        return current != null && current.openNearby(player, radius);
    }

    /**
     * Moves the player's whole inventory into a new tombstone at {@code location}, without a death.
     * Returns false (and leaves the inventory untouched) when the module is off or there is nothing to bury.
     */
    public boolean bury(Player player, Location location) {
        TombstoneRuntime current = runtime;
        if (current == null) return false;
        ItemStack cursor = player.getItemOnCursor();
        // The native player data file does not contain the cursor stack.
        if (cursor != null && !cursor.getType().isAir()) return false;
        if (player.getOpenInventory().getTopInventory() instanceof CraftingInventory crafting
            && Arrays.stream(crafting.getMatrix()).anyMatch(item -> item != null && !item.getType().isAir())) return false;
        List<ItemStack> items = TombstoneItemLayout.snapshot(
            player.getInventory().getStorageContents(),
            player.getInventory().getArmorContents(),
            player.getInventory().getItemInOffHand(),
            null
        );
        return current.bury(player, location, items);
    }

    public boolean capturesDeathOf(UUID playerId) {
        return shouldCaptureDeath(playerId);
    }

    public void setDeathCaptureEligibility(Predicate<UUID> eligibility) {
        deathCaptureEligibility = eligibility == null ? ignored -> true : eligibility;
    }

    private boolean shouldCaptureDeath(UUID playerId) {
        return deathCaptureEligibility.test(playerId);
    }
}
