package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ShapedRecipe;

public final class CopperAppleModule implements PluginModule {

    private final BigCasares plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;

    private CopperAppleItem copperAppleItem;

    public CopperAppleModule(BigCasares plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "copper_apple");
        this.recipeKey = new NamespacedKey(plugin, "copper_apple_recipe");
    }

    @Override
    public String getId() {
        return "copper-apple";
    }

    @Override
    public void onEnable() {
        this.copperAppleItem = new CopperAppleItem(itemKey);
        plugin.getCustomItemRegistry().register(copperAppleItem);

        registerRecipe();
        registerListeners();
        registerCommands();
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(recipeKey);
        plugin.getCustomItemRegistry().unregister(CopperAppleItem.ID);
    }

    private void registerRecipe() {
        Bukkit.removeRecipe(recipeKey);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, copperAppleItem.createItemStack(1));
        recipe.shape("CCC", "CAC", "CCC");
        recipe.setIngredient('C', Material.COPPER_INGOT);
        recipe.setIngredient('A', Material.APPLE);

        Bukkit.addRecipe(recipe);
    }

    private void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(
            new CopperAppleCraftListener(recipeKey, copperAppleItem),
            plugin
        );
    }

    private void registerCommands() {
        PluginCommand command = plugin.getCommand("bigcasares");
        if (command == null) {
            plugin.getLogger().warning("Command 'bigcasares' is not declared in plugin.yml");
            return;
        }

        CopperAppleCommand handler = new CopperAppleCommand(plugin);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }
}
