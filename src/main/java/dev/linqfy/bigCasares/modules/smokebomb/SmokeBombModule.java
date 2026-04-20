package dev.linqfy.bigCasares.modules.smokebomb;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ShapedRecipe;

public final class SmokeBombModule implements PluginModule {

    private final BigCasares plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;

    private SmokeBombItem smokeBombItem;
    private SmokeBombProjectileListener projectileListener;

    public SmokeBombModule(BigCasares plugin) {
        this.plugin = plugin;
        this.itemKey = plugin == null ? null : new NamespacedKey(plugin, "smoke_bomb");
        this.recipeKey = plugin == null ? null : new NamespacedKey(plugin, "smoke_bomb_recipe");
    }

    @Override
    public String getId() {
        return "smoke-bomb";
    }

    @Override
    public void onEnable() {
        SmokeBombSettings settings = SmokeBombSettings.load(plugin.getConfig());
        SmokeCloudService cloudService = new SmokeCloudService(settings);
        SmokeConcealmentService concealmentService = new SmokeConcealmentService(plugin);

        this.smokeBombItem = new SmokeBombItem(itemKey);
        this.projectileListener = new SmokeBombProjectileListener(
            plugin,
            smokeBombItem,
            settings,
            cloudService,
            concealmentService
        );

        plugin.getCustomItemRegistry().register(smokeBombItem);
        registerRecipe();
        registerListeners(concealmentService);
    }

    @Override
    public void onDisable() {
        if (projectileListener != null) {
            projectileListener.shutdown();
        }
        if (recipeKey != null) {
            Bukkit.removeRecipe(recipeKey);
        }
        plugin.getCustomItemRegistry().unregister(SmokeBombItem.ID);
    }

    private void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, smokeBombItem.createItemStack(1));
        recipe.shape("PTP", "TRT", "PTP");
        recipe.setIngredient('P', Material.GUNPOWDER);
        recipe.setIngredient('T', Material.INK_SAC);
        recipe.setIngredient('R', Material.REDSTONE);
        Bukkit.addRecipe(recipe);
    }

    private void registerListeners(SmokeConcealmentService concealmentService) {
        plugin.getServer().getPluginManager().registerEvents(
            new SmokeBombCraftListener(recipeKey, smokeBombItem),
            plugin
        );
        plugin.getServer().getPluginManager().registerEvents(projectileListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(
            new SmokeBombVisibilityListener(concealmentService),
            plugin
        );
    }
}
