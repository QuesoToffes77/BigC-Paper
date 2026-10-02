package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import dev.linqfy.bigCasares.modules.model.BetterModelAssetInstaller;
import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import org.bukkit.command.PluginCommand;
import org.bukkit.scheduler.BukkitTask;

import java.time.Clock;
import java.util.logging.Level;

public final class AcidRainModule implements PluginModule {
    public static final String MODULE_ID = "acid-rain";

    private final BigCasares plugin;
    private final JavaModelGateway models;
    private AcidRainRuntime runtime;
    private RuntimeRegistrationScope compatibilityScope;
    private AcidRainConfigLoadResult lastConfigLoad = new AcidRainConfigLoadResult(
        AcidRainSettings.safeDefaults(), java.util.List.of(), java.util.List.of()
    );

    public AcidRainModule(BigCasares plugin) {
        this(plugin, unavailableModels());
    }

    public AcidRainModule(BigCasares plugin, JavaModelGateway models) {
        this.plugin = plugin;
        this.models = java.util.Objects.requireNonNull(models, "models");
    }

    private static JavaModelGateway unavailableModels() {
        return new JavaModelGateway() {
            @Override
            public JavaModelHandle attach(org.bukkit.entity.Entity anchor, String modelKey) {
                throw new IllegalStateException("Java model gateway is required for toxic mob rendering");
            }

            @Override
            public boolean animate(JavaModelHandle handle, String animationKey) {
                throw new IllegalStateException("Java model gateway is required for toxic mob rendering");
            }

            @Override
            public boolean animateOnce(JavaModelHandle handle, String animationKey, Runnable onEnd) {
                throw new IllegalStateException("Java model gateway is required for toxic mob rendering");
            }

            @Override
            public boolean scale(JavaModelHandle handle, float factor) {
                throw new IllegalStateException("Java model gateway is required for toxic mob rendering");
            }

            @Override
            public void close(JavaModelHandle handle) {
                throw new IllegalStateException("Java model gateway is required for toxic mob rendering");
            }
        };
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
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        lastConfigLoad = AcidRainSettingsLoader.load(plugin.getConfig());
        logLoadResult(lastConfigLoad);

        BetterModelAssetInstaller.installToxicMobModel(plugin);
        // BetterModel may still be initializing right after plugin enable; the
        // delayed pass repairs the model registry like the PvE module does.
        BukkitTask delayedModelReload = plugin.getServer().getScheduler().runTaskLater(plugin,
            () -> BetterModelAssetInstaller.installToxicMobModel(plugin), 60L);
        scope.register("acid-rain-toxic-model-delayed-reload", delayedModelReload::cancel);

        runtime = new AcidRainRuntime(plugin, lastConfigLoad.settings(), lastConfigLoad, Clock.systemUTC(), models);
        AcidRainRuntime ownedRuntime = runtime;
        scope.register("acid-rain-runtime", ownedRuntime::shutdown);

        AcidRainCommand command = new AcidRainCommand(this);
        PluginCommand pluginCommand = plugin.getCommand("acidrain");
        if (pluginCommand == null) {
            throw new IllegalStateException("Required command is not declared: acidrain");
        }
        registrations.bindCommand("acid-rain-command", pluginCommand, command, command);
        registrations.registerListener("acid-rain-exposure-listener", new AcidRainExposureListener(ownedRuntime));
        registrations.registerListener("acid-rain-mob-listener", new AcidRainMobListener(ownedRuntime));
        registrations.scheduleRepeating("acid-rain-lifecycle", ownedRuntime::tickLifecycle, 20L, 20L);
        registrations.scheduleRepeating("acid-rain-damage", ownedRuntime::tickDamage, 20L, 20L);
        registrations.scheduleRepeating("acid-rain-feedback", ownedRuntime::tickFeedback, 10L, 10L);
        // 10-tick cadence so level-specific destruction intervals (ACID 40,
        // TOXIC 20, CHEMICAL 10 ticks) can express real frequency differences
        // while the environment service still enforces max-blocks-per-second.
        registrations.scheduleRepeating("acid-rain-environment", ownedRuntime::tickEnvironment, 20L, 10L);
        // Same 10-tick cadence as the environment task. The logical mob
        // scheduler only spawns while ACTIVE, so this single task is safe to
        // keep registered for the whole module lifecycle.
        registrations.scheduleRepeating("acid-rain-mobs", ownedRuntime::tickMobs, 20L, 10L);

        plugin.getLogger().info("[AcidRain] Module enabled.");
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

    public AcidRainStartResult start(AcidRainLevel level) {
        requireRuntime();
        return runtime.start(level);
    }

    public AcidRainTransitionResult stop(String reason) {
        requireRuntime();
        return runtime.stop(reason);
    }

    public AcidRainConfigLoadResult reloadSettings() {
        requireRuntime();
        plugin.reloadConfig();
        AcidRainConfigLoadResult loaded = AcidRainSettingsLoader.load(plugin.getConfig());
        logLoadResult(loaded);
        if (loaded.valid()) {
            runtime.updateSettings(loaded.settings(), loaded);
            lastConfigLoad = loaded;
            return loaded;
        }
        runtime.markConfigLoad(loaded);
        lastConfigLoad = loaded;
        return loaded;
    }

    public java.util.List<String> infoLines() {
        requireRuntime();
        return runtime.infoLines(lastConfigLoad);
    }

    public AcidRainSnapshot snapshot() {
        requireRuntime();
        return runtime.snapshot();
    }

    public boolean isEventRunning() {
        return runtime != null && runtime.snapshot().state() != AcidRainState.INACTIVE;
    }

    AcidRainRuntime runtime() {
        return runtime;
    }

    private void requireRuntime() {
        if (runtime == null) {
            throw new IllegalStateException("Acid Rain no esta disponible.");
        }
    }

    private void logLoadResult(AcidRainConfigLoadResult load) {
        for (String warning : load.warnings()) {
            plugin.getLogger().warning("[AcidRain] " + warning);
        }
        for (String error : load.errors()) {
            plugin.getLogger().severe("[AcidRain] " + error);
        }
        if (!load.valid()) {
            plugin.getLogger().log(Level.WARNING, "[AcidRain] Invalid configuration loaded in fail-safe mode.");
        }
    }
}
