package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.NamespacedKey;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Independent Grappling Hook module: owns its six catalog items, its listener,
 * its cooldown state and its lazily-started one-tick runtime task.
 *
 * <p>No BetterModel entity is spawned: the held item uses its resource-pack
 * 3D model directly. The {@link GrapplingHookModelManager} is a
 * lightweight no-op stub kept so that callers can forward shoot/reload events
 * without null-checks.
 */
public final class GrapplingHookModule implements PluginModule {

    public static final String MODULE_ID = "grappling-hook";

    private final BigCasares plugin;
    private final Map<GrapplingHookTier, NamespacedKey> itemKeys = new EnumMap<>(GrapplingHookTier.class);
    private GrapplingHookRuntime runtime;
    private GrappleCooldownService cooldowns;
    private GrapplingHookModelManager modelManager;
    private RuntimeRegistrationScope compatibilityScope;

    public GrapplingHookModule(BigCasares plugin) {
        this(plugin, null);
    }

    public GrapplingHookModule(BigCasares plugin, @SuppressWarnings("unused") Object javaModels) {
        this.plugin = plugin;
        if (plugin != null) {
            for (GrapplingHookTier tier : GrapplingHookTier.values()) {
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
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        if (plugin == null) {
            return;
        }
        GrapplingHookSettings settings = GrapplingHookSettings.load(plugin.getConfig(),
            plugin.getLogger()::warning);
        if (!settings.canUse()) {
            plugin.getLogger().info("[GrapplingHook] Module disabled by config.");
            return;
        }
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        GrappleCooldownService ownedCooldowns = new GrappleCooldownService();
        this.cooldowns = ownedCooldowns;

        // Lightweight no-op model manager: the item uses its 3D resource-pack
        // model; no BetterModel entity or tracker is needed.
        GrapplingHookModelManager ownedModelManager = new GrapplingHookModelManager();
        this.modelManager = ownedModelManager;

        GrapplingHookRuntime ownedRuntime = new GrapplingHookRuntime(plugin, settings, ownedCooldowns, modelManager);
        this.runtime = ownedRuntime;

        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            GrapplingHookItem item = new GrapplingHookItem(tier, plugin.getCustomItemRegistry(), itemKeys.get(tier));
            plugin.getCustomItemRegistry().register(item);
            scope.register("grappling-hook-item-" + tier.name().toLowerCase(Locale.ROOT),
                () -> plugin.getCustomItemRegistry().unregister(tier.catalogId()));
        }

        registrations.registerListener("grappling-hook-listener",
            new GrapplingHookListener(plugin.getCustomItemRegistry(), settings, ownedCooldowns, ownedRuntime,
                modelManager));
        scope.register("grappling-hook-runtime-state", ownedRuntime::close);
        scope.register("grappling-hook-cooldowns", ownedCooldowns::clear);
        plugin.getLogger().info("[GrapplingHook] Module enabled (6 tiers, MAIN_HAND + OFF_HAND).");
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
        cooldowns = null;
        modelManager = null;
    }
}
