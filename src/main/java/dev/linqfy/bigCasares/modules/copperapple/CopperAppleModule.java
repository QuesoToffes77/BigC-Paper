package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.NamespacedKey;

public final class CopperAppleModule implements PluginModule {

    private final BigCasares plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;

    private CopperAppleItem copperAppleItem;
    private RuntimeRegistrationScope compatibilityScope;

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
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        this.copperAppleItem = new CopperAppleItem(plugin.getCustomItemRegistry(), itemKey);
        plugin.getCustomItemRegistry().register(copperAppleItem);
        scope.register("custom-item", () -> plugin.getCustomItemRegistry().unregister(CopperAppleItem.ID));

        registrations.registerListener("craft-listener", new CopperAppleCraftListener(recipeKey, copperAppleItem));
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        copperAppleItem = null;
    }

}
