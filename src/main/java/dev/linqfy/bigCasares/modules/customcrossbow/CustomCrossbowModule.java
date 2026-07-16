package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.NamespacedKey;

public final class CustomCrossbowModule implements PluginModule {

    private final BigCasares plugin;
    private final NamespacedKey chargeTypeKey;
    private final NamespacedKey fireworkPowerKey;
    private final NamespacedKey chargeCountKey;
    private final NamespacedKey originalItemModelKey;
    private final NamespacedKey prismarineArrowKey;
    private final NamespacedKey recipeKey;

    private PrismarineArrowItem prismarineArrowItem;
    private PrismarineArrowListener prismarineArrowListener;
    private CustomCrossbowChargeListener chargeListener;
    private RuntimeRegistrationScope compatibilityScope;

    public CustomCrossbowModule(BigCasares plugin) {
        this.plugin = plugin;
        this.chargeTypeKey = plugin == null ? null : new NamespacedKey(plugin, "custom_crossbow_charge");
        this.fireworkPowerKey = plugin == null ? null : new NamespacedKey(plugin, "custom_crossbow_firework_power");
        this.chargeCountKey = plugin == null ? null : new NamespacedKey(plugin, "custom_crossbow_charge_count");
        this.originalItemModelKey = plugin == null ? null : new NamespacedKey(plugin, "custom_crossbow_original_item_model");
        this.prismarineArrowKey = plugin == null ? null : new NamespacedKey(plugin, "prismarine_arrow");
        this.recipeKey = plugin == null ? null : new NamespacedKey(plugin, "prismarine_arrow_recipe");
    }

    @Override
    public String getId() {
        return "custom-crossbow";
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        CustomCrossbowSettings settings = CustomCrossbowSettings.load(plugin.getConfig());
        CustomCrossbowData crossbowData = new CustomCrossbowData(
            chargeTypeKey,
            fireworkPowerKey,
            chargeCountKey,
            originalItemModelKey
        );
        CustomCrossbowLoadService loadService = new CustomCrossbowLoadService();
        CustomCrossbowDurabilityService durabilityService = new CustomCrossbowDurabilityService();
        EchoShardCooldownService echoShardCooldownService = new EchoShardCooldownService(settings.echoShardCooldownTicks());
        this.prismarineArrowItem = new PrismarineArrowItem(plugin.getCustomItemRegistry(), prismarineArrowKey);
        this.prismarineArrowListener = new PrismarineArrowListener(prismarineArrowItem, registrations);
        this.chargeListener = new CustomCrossbowChargeListener(plugin, crossbowData, settings, loadService, registrations);

        plugin.getCustomItemRegistry().register(prismarineArrowItem);
        scope.register("custom-item", () -> plugin.getCustomItemRegistry().unregister(PrismarineArrowItem.ID));
        scope.register("prismarine-runtime", prismarineArrowListener::shutdown);
        scope.register("charge-runtime", chargeListener::shutdown);
        registerListeners(registrations, crossbowData, settings, durabilityService, echoShardCooldownService);
    }

    @Override
    public void onDisable() {
        if (prismarineArrowListener != null) {
            prismarineArrowListener.shutdown();
        }
        if (chargeListener != null) {
            chargeListener.shutdown();
        }
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        prismarineArrowListener = null;
        chargeListener = null;
        prismarineArrowItem = null;
    }

    private void registerListeners(
        BukkitRuntimeRegistrations registrations,
        CustomCrossbowData crossbowData,
        CustomCrossbowSettings settings,
        CustomCrossbowDurabilityService durabilityService,
        EchoShardCooldownService echoShardCooldownService
    ) {
        registrations.registerListener("charge-listener", chargeListener);
        registrations.registerListener(
            "shoot-listener",
            new CustomCrossbowShootListener(
                plugin,
                crossbowData,
                prismarineArrowItem,
                settings,
                durabilityService,
                echoShardCooldownService,
                prismarineArrowListener::track,
                registrations
            )
        );
        registrations.registerListener("prismarine-arrow-listener", prismarineArrowListener);
        registrations.registerListener("durability-listener", new CustomCrossbowDurabilityListener(durabilityService));
        registrations.registerListener("inventory-limit-listener", new CustomCrossbowInventoryLimitListener(crossbowData, settings));
        registrations.registerListener("craft-listener", new PrismarineArrowCraftListener(recipeKey, prismarineArrowItem));
        registrations.registerListener("loot-listener", new CustomCrossbowLootListener(settings));
    }

}
