package dev.linqfy.bigCasares.modules.jeremy;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class JeremyModule implements PluginModule {
    public static final String MODULE_ID = "jeremy";
    private final BigCasares plugin;
    private JeremyRuntime runtime;
    private RuntimeRegistrationScope compatibilityScope;
    private JeremyConfigLoadResult configLoad = new JeremyConfigLoadResult(
        JeremySettings.disabled(), List.of(), List.of());

    public JeremyModule(BigCasares plugin) {
        this.plugin = plugin;
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
        configLoad = JeremySettingsLoader.load(plugin.getConfig());
        configLoad.warnings().forEach(warning -> plugin.getLogger().warning("[Jeremy] " + warning));
        configLoad.errors().forEach(error -> plugin.getLogger().severe("[Jeremy] " + error));

        Path storagePath = plugin.getDataFolder().toPath().resolve("jeremy-state.yml");
        runtime = new JeremyRuntime(plugin, configLoad.settings(), new YamlJeremyStorage(storagePath));
        JeremyRuntime ownedRuntime = runtime;
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        registrations.ownCleanup("jeremy-runtime", ownedRuntime::shutdown);
        registrations.registerListener("jeremy-listener", new JeremyListener(ownedRuntime));
        registrations.scheduleRepeating("jeremy-runtime-task", ownedRuntime::tick, 2L, 2L);

        PluginCommand pluginCommand = plugin.getCommand("jeremy");
        if (pluginCommand == null) {
            throw new IllegalStateException("Required command is not declared: jeremy");
        }
        JeremyCommand command = new JeremyCommand(this);
        registrations.bindCommand("jeremy-command", pluginCommand, command, command);
        plugin.getLogger().info("[Jeremy] Module enabled (config "
            + (configLoad.valid() ? "valid" : "disabled fail-safe") + ").");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.shutdown();
        }
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        runtime = null;
    }

    JeremyActionResult start(Player target) {
        return requireRuntime().start(target);
    }

    JeremyActionResult stop() {
        return requireRuntime().stop();
    }

    JeremyActionResult resetCooldown() {
        return requireRuntime().resetCooldown();
    }

    List<String> statusLines() {
        return requireRuntime().statusLines();
    }

    Server server() {
        return Objects.requireNonNull(plugin, "plugin").getServer();
    }

    public boolean isHunting() {
        return runtime != null && runtime.phase() == JeremyPhase.HUNTING;
    }

    private JeremyRuntime requireRuntime() {
        if (runtime == null) {
            throw new IllegalStateException("Jeremy no esta disponible.");
        }
        return runtime;
    }
}
