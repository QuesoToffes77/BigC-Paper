package dev.linqfy.bigCasares.modules.bloodmoon;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class BloodMoonModule implements PluginModule {
    public static final String MODULE_ID = "blood-moon";

    private final BigCasares plugin;
    private final BooleanSupplier otherEventActive;
    private BloodMoonRuntime runtime;
    private RuntimeRegistrationScope compatibilityScope;
    private BloodMoonConfigLoadResult configLoad = new BloodMoonConfigLoadResult(
        BloodMoonSettings.disabled(), List.of(), List.of());

    public BloodMoonModule(BigCasares plugin, BooleanSupplier otherEventActive) {
        this.plugin = plugin;
        this.otherEventActive = otherEventActive == null ? () -> false : otherEventActive;
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
        configLoad = BloodMoonSettingsLoader.load(plugin.getConfig());
        configLoad.warnings().forEach(warning -> plugin.getLogger().warning("[BloodMoon] " + warning));
        configLoad.errors().forEach(error -> plugin.getLogger().severe("[BloodMoon] " + error));

        runtime = new BloodMoonRuntime(plugin, configLoad.settings(), otherEventActive);
        BloodMoonRuntime ownedRuntime = runtime;
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        registrations.ownCleanup("blood-moon-runtime", ownedRuntime::shutdown);
        registrations.registerListener("blood-moon-listener", new BloodMoonListener(ownedRuntime));
        registrations.scheduleRepeating("blood-moon-runtime-task", ownedRuntime::tick, 10L, 10L);

        PluginCommand pluginCommand = plugin.getCommand("bloodmoon");
        if (pluginCommand == null) {
            throw new IllegalStateException("Required command is not declared: bloodmoon");
        }
        BloodMoonCommand command = new BloodMoonCommand(this);
        registrations.bindCommand("blood-moon-command", pluginCommand, command, command);
        plugin.getLogger().info("[BloodMoon] Module enabled (config "
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

    BloodMoonActionResult start(World world) {
        return requireRuntime().start(world);
    }

    BloodMoonActionResult stop(World world) {
        return requireRuntime().stop(world);
    }

    int stopAll() {
        return requireRuntime().stopAll();
    }

    List<String> statusLines() {
        return requireRuntime().statusLines();
    }

    public boolean isActive() {
        return runtime != null && !runtime.activeWorlds().isEmpty();
    }

    Server server() {
        return Objects.requireNonNull(plugin, "plugin").getServer();
    }

    private BloodMoonRuntime requireRuntime() {
        if (runtime == null) {
            throw new IllegalStateException("Blood Moon no esta disponible.");
        }
        return runtime;
    }
}
