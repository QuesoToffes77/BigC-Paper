package dev.linqfy.bigCasares.modules.endevent;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;

import java.time.Clock;
import java.util.UUID;

public final class EndEventModule implements PluginModule {
    private final BigCasares plugin;
    private EndEventRuntime runtime;
    private RuntimeRegistrationScope compatibilityScope;

    public EndEventModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "end-event-system";
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
        EndEventSettings settings = EndEventSettings.load(plugin.getConfig());
        YamlEndEventStorage storage = new YamlEndEventStorage(
            plugin.getDataFolder().toPath().resolve("data/end-event-system/state.yml")
        );
        EndEventService service = new EndEventService(storage, Clock.systemUTC());
        runtime = new EndEventRuntime(
            plugin, settings, storage, service, registrations, Clock.systemUTC()
        );
        registrations.registerListener("end-event-listener", runtime);
        var command = plugin.getCommand("endevent");
        if (command == null) {
            throw new IllegalStateException("Required command is not declared: endevent");
        }
        EndEventCommand handler = new EndEventCommand(runtime);
        registrations.bindCommand("end-event-command", command, handler, handler);
        registrations.scheduleRepeating("end-event-heartbeat", runtime::heartbeat, 1L, 20L);
        registrations.scheduleRepeating("end-event-fast-tick", runtime::fastTick, 1L, 2L);
        scope.register("end-event-runtime", () -> {
            if (runtime != null) {
                runtime.shutdown();
                runtime = null;
            }
        });
        runtime.restoreLiveState();
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
    }

    public boolean bypassTombstone(UUID playerId) {
        return runtime != null && runtime.bypassTombstone(playerId);
    }

    public EndEventPhase phase() {
        return runtime == null ? EndEventPhase.ARMED : runtime.phase();
    }
}
