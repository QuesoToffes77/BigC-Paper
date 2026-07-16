package dev.linqfy.bigCasares.modules.smokebomb;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.NamespacedKey;

public final class SmokeBombModule implements PluginModule {

    private final BigCasares plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;

    private SmokeBombItem smokeBombItem;
    private SmokeBombProjectileListener projectileListener;
    private RuntimeRegistrationScope compatibilityScope;

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
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        SmokeBombSettings settings = SmokeBombSettings.load(plugin.getConfig());
        SmokeCloudService cloudService = new SmokeCloudService(settings);
        SmokeConcealmentService concealmentService = new SmokeConcealmentService(plugin);

        this.smokeBombItem = new SmokeBombItem(plugin.getCustomItemRegistry(), itemKey);
        this.projectileListener = new SmokeBombProjectileListener(
            plugin,
            smokeBombItem,
            settings,
            cloudService,
            concealmentService,
            registrations
        );

        plugin.getCustomItemRegistry().register(smokeBombItem);
        scope.register("custom-item", () -> plugin.getCustomItemRegistry().unregister(SmokeBombItem.ID));
        scope.register("projectile-runtime", projectileListener::shutdown);
        registerListeners(registrations, concealmentService);
    }

    @Override
    public void onDisable() {
        if (projectileListener != null) {
            projectileListener.shutdown();
        }
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        projectileListener = null;
        smokeBombItem = null;
    }

    private void registerListeners(
        BukkitRuntimeRegistrations registrations,
        SmokeConcealmentService concealmentService
    ) {
        registrations.registerListener("craft-listener", new SmokeBombCraftListener(recipeKey, smokeBombItem));
        registrations.registerListener("projectile-listener", projectileListener);
        registrations.registerListener("visibility-listener", new SmokeBombVisibilityListener(concealmentService));
    }
}
