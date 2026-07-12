package dev.linqfy.bigCasares.modules.model;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class JavaModelGatewayFactory {
    private static final String BETTER_MODEL = "BetterModel";
    private static final String REQUIRED_MESSAGE = "BetterModel is required. Install BetterModel 3.2.0 before starting BigCasares.";

    private JavaModelGatewayFactory() {
    }

    public static JavaModelGateway create(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return create(plugin.getServer().getPluginManager().getPlugin(BETTER_MODEL));
    }

    static JavaModelGateway create(Plugin betterModel) {
        if (betterModel == null || !betterModel.isEnabled()) {
            throw new IllegalStateException(REQUIRED_MESSAGE);
        }
        return new BetterModelJavaModelGateway();
    }
}
