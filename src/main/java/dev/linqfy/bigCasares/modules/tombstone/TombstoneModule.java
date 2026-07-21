package dev.linqfy.bigCasares.modules.tombstone;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;

import java.time.Clock;
import java.time.Duration;
import org.bukkit.entity.Player;

public final class TombstoneModule implements PluginModule {
    private final BigCasares plugin;
    private TombstoneRuntime runtime;
    private RuntimeRegistrationScope compatibilityScope;

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
        this.runtime = new TombstoneRuntime(
            plugin,
            new YamlTombstoneStorage(plugin.getDataFolder().toPath().resolve("data/tombstones/tombstones.yml")),
            Clock.systemUTC(), Duration.ofSeconds(lifetimeSeconds), registrations
        );
        registrations.registerListener("tombstone-listener", new TombstoneListener(runtime, registrations));
        runtime.load();
        scope.register("tombstone-runtime", () -> {
            if (runtime != null) runtime.shutdown();
            runtime = null;
        });
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
}
