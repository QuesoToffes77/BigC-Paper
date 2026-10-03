package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** Six-tier, catalog-backed Glider module supporting either equipped hand. */
public final class GliderModule implements PluginModule {

    public static final String MODULE_ID = "glider";

    private final BigCasares plugin;
    private final Map<GliderTier, NamespacedKey> itemKeys = new EnumMap<>(GliderTier.class);
    private RuntimeRegistrationScope compatibilityScope;
    private GliderRuntime runtime;

    public GliderModule(BigCasares plugin) {
        this.plugin = plugin;
        if (plugin != null) {
            for (GliderTier tier : GliderTier.values()) {
                itemKeys.put(tier, new NamespacedKey(plugin, tier.catalogId()));
            }
        }
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
            return;
        }
        GliderSettings settings = GliderSettings.load(plugin.getConfig(), plugin.getLogger()::warning);
        if (!settings.enabled()) {
            plugin.getLogger().info("[Glider] Module disabled by config.");
            return;
        }

        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        for (GliderTier tier : GliderTier.values()) {
            plugin.getCustomItemRegistry().register(
                new GliderItem(tier, plugin.getCustomItemRegistry(), itemKeys.get(tier)));
            scope.register("glider-item-" + tier.name().toLowerCase(Locale.ROOT),
                () -> plugin.getCustomItemRegistry().unregister(tier.catalogId()));
        }

        GliderRuntime ownedRuntime = new GliderRuntime(plugin, plugin.getCustomItemRegistry(), settings);
        runtime = ownedRuntime;
        GliderCommand command = new GliderCommand(plugin);
        PluginCommand pluginCommand = plugin.getCommand("glider");
        if (pluginCommand == null) {
            throw new IllegalStateException("Required command is not declared: glider");
        }
        registrations.bindCommand("glider-command", pluginCommand, command, command);
        registrations.registerListener("glider-listener", new GliderListener(ownedRuntime));
        registrations.scheduleRepeating("glider-runtime", ownedRuntime, 1L, settings.tickRate());
        scope.register("glider-runtime-state", ownedRuntime::close);
        plugin.getLogger().info("[Glider] Module enabled (6 tiers, MAIN_HAND + OFF_HAND).");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
        }
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        runtime = null;
    }
}
