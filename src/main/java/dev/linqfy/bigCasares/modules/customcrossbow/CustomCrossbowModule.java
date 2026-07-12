package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

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
        this.prismarineArrowItem = new PrismarineArrowItem(prismarineArrowKey);
        this.prismarineArrowListener = new PrismarineArrowListener(plugin, prismarineArrowItem);
        this.chargeListener = new CustomCrossbowChargeListener(plugin, crossbowData, settings, loadService);

        plugin.getCustomItemRegistry().register(prismarineArrowItem);
        registerRecipe();
        registerListeners(crossbowData, settings, durabilityService, echoShardCooldownService);
    }

    @Override
    public void onDisable() {
        if (prismarineArrowListener != null) {
            prismarineArrowListener.shutdown();
        }
        if (chargeListener != null) {
            chargeListener.shutdown();
        }
        if (recipeKey != null) {
            Bukkit.removeRecipe(recipeKey);
        }
        plugin.getCustomItemRegistry().unregister(PrismarineArrowItem.ID);
    }

    private void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, prismarineArrowItem.createItemStack(4));
        recipe.shape(" P ", "PAP", " P ");
        recipe.setIngredient('P', Material.PRISMARINE_SHARD);
        recipe.setIngredient('A', Material.ARROW);
        Bukkit.addRecipe(recipe);
    }

    private void registerListeners(
        CustomCrossbowData crossbowData,
        CustomCrossbowSettings settings,
        CustomCrossbowDurabilityService durabilityService,
        EchoShardCooldownService echoShardCooldownService
    ) {
        plugin.getServer().getPluginManager().registerEvents(chargeListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new CustomCrossbowShootListener(
                plugin,
                crossbowData,
                prismarineArrowItem,
                settings,
                durabilityService,
                echoShardCooldownService,
                prismarineArrowListener::track
            ),
            plugin
        );
        plugin.getServer().getPluginManager().registerEvents(prismarineArrowListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new CustomCrossbowDurabilityListener(durabilityService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CustomCrossbowInventoryLimitListener(crossbowData, settings), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PrismarineArrowCraftListener(recipeKey, prismarineArrowItem), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CustomCrossbowLootListener(settings), plugin);
    }

}
