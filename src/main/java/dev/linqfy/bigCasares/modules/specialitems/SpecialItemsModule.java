package dev.linqfy.bigCasares.modules.specialitems;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.NamespacedKey;

public final class SpecialItemsModule implements PluginModule {

    public static final String MODULE_ID = "special-items";

    private final BigCasares plugin;
    private RuntimeRegistrationScope compatibilityScope;

    public SpecialItemsModule(BigCasares plugin) {
        this.plugin = plugin;
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
        if (plugin == null) {
            throw new IllegalStateException("SpecialItemsModule needs a plugin instance before it can be enabled");
        }
        TrackerCompassItem tracker = new TrackerCompassItem(
            plugin.getCustomItemRegistry(), new NamespacedKey(plugin, TrackerCompassItem.ID));
        NukeShotItem nukeShot = new NukeShotItem(
            plugin.getCustomItemRegistry(), new NamespacedKey(plugin, NukeShotItem.ID));
        SpecialItemsSettings settings = SpecialItemsSettings.load(plugin.getConfig());
        TrackerCompassService trackerService = new TrackerCompassService(
            settings.trackingMillis(), settings.cooldownMillis());
        NukeAnimationRuntime nukeRuntime = new NukeAnimationRuntime(plugin, settings);
        plugin.getCustomItemRegistry().register(tracker);
        scope.register("tracker-compass-item",
            () -> plugin.getCustomItemRegistry().unregister(TrackerCompassItem.ID));
        plugin.getCustomItemRegistry().register(nukeShot);
        scope.register("nuke-shot-item", () -> plugin.getCustomItemRegistry().unregister(NukeShotItem.ID));

        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        registrations.registerListener("tracker-compass-listener",
            new TrackerCompassListener(tracker, trackerService));
        registrations.registerListener("nuke-shot-listener", new NukeShotListener(nukeShot, nukeRuntime));
        registrations.scheduleRepeating("tracker-compass-runtime",
            new TrackerCompassRuntime(plugin, tracker, trackerService),
            settings.trackerUpdateTicks(), settings.trackerUpdateTicks());
        registrations.scheduleRepeating("nuke-animation-runtime", nukeRuntime, 1L, 1L);
        scope.register("tracker-compass-state", trackerService::clear);
        scope.register("nuke-animation-state", nukeRuntime::close);
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
    }
}
