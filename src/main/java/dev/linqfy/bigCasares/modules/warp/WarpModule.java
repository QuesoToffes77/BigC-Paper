package dev.linqfy.bigCasares.modules.warp;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;

public final class WarpModule implements PluginModule {
    private final BigCasares plugin;
    private WarpStorage storage;
    private RuntimeRegistrationScope compatibilityScope;

    public WarpModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "warp-system";
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        this.storage = new WarpStorage(plugin.getDataFolder().toPath().resolve("data/warps.yml"));
        this.storage.load();

        registrations.registerListener("warp-listener", new WarpListener(this));

        scope.register("warp-storage", () -> {
            storage = null;
        });
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) scope.close();
    }

    public WarpStorage getStorage() {
        return storage;
    }

    public BigCasares getPlugin() {
        return plugin;
    }
}
