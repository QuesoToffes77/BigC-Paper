package dev.linqfy.bigCasares.modules.teams;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;

public final class TeamModule implements PluginModule {

    private final BigCasares plugin;
    private TeamService service;
    private TeamPresentationService presentationService;
    private TeamInvitationService invitationService;
    private TeamCommandService commandService;
    private TeamPresentationListener listener;
    private Object placeholderExpansion;
    private RuntimeRegistrationScope compatibilityScope;

    public TeamModule() {
        this(null);
    }

    public TeamModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "team-system";
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        if (plugin == null) {
            return;
        }
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        scope.register("module-state", this::clearRuntimeState);
        TeamTagValidator.configureAllowedSymbols(plugin.getConfig().getStringList("team-system.tag.allowed-symbols"));
        File file = new File(plugin.getDataFolder(), "data/teams/teams.yml");
        this.service = new TeamService(new YamlTeamStorage(file));
        ConfigurationSection presentation = plugin.getConfig().getConfigurationSection("team-system.presentation");
        boolean scoreboard = presentation == null || presentation.getBoolean("scoreboard-prefix", true);
        boolean tab = presentation == null || presentation.getBoolean("tab-list-name", true);
        boolean display = presentation == null || presentation.getBoolean("display-name", true);
        BukkitTeamNamePresentationGateway gateway = new BukkitTeamNamePresentationGateway(
            plugin.getServer(), service, scoreboard, tab, display
        );
        this.presentationService = new TeamPresentationService(service, gateway);
        this.invitationService = new TeamInvitationService(Clock.systemUTC(), Duration.ofMinutes(5));
        this.commandService = new TeamCommandService(
            service, presentationService, invitationService, Clock.systemUTC());
        this.listener = new TeamPresentationListener(presentationService);
        registrations.registerListener("team-presentation-listener", listener);
        plugin.getServer().getOnlinePlayers().forEach(player ->
            presentationService.refreshPlayer(player.getUniqueId()));
        registerPlaceholderExpansion(scope);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        unregisterPlaceholderExpansion();
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        clearRuntimeState();
    }

    public BigCasares plugin() {
        return plugin;
    }

    public Optional<TeamService> service() {
        return Optional.ofNullable(service);
    }

    public Optional<TeamPresentationService> presentationService() {
        return Optional.ofNullable(presentationService);
    }

    public Optional<TeamCommandService> commandService() {
        return Optional.ofNullable(commandService);
    }

    private void registerPlaceholderExpansion(RuntimeRegistrationScope scope) {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI no está instalado; Team System continúa sin placeholders.");
            return;
        }
        try {
            Class<?> type = Class.forName(
                "dev.linqfy.bigCasares.modules.teams.BigCasaresTeamPlaceholderExpansion",
                true,
                plugin.getClass().getClassLoader()
            );
            Object expansion = type.getConstructor(TeamService.class, java.util.function.Function.class)
                .newInstance(service, (java.util.function.Function<java.util.UUID, String>) id -> {
                    org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(id);
                    return player.getName() == null ? id.toString() : player.getName();
                });
            boolean registered = (boolean) type.getMethod("register").invoke(expansion);
            if (registered) {
                placeholderExpansion = expansion;
                scope.register("placeholder-expansion", this::unregisterPlaceholderExpansion);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException | LinkageError ex) {
            plugin.getLogger().log(Level.WARNING,
                "PlaceholderAPI está instalado pero la expansión de equipos no pudo registrarse.", ex);
        }
    }

    private void unregisterPlaceholderExpansion() {
        Object expansion = placeholderExpansion;
        placeholderExpansion = null;
        if (expansion == null) {
            return;
        }
        try {
            expansion.getClass().getMethod("unregister").invoke(expansion);
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.FINE, "No se pudo desregistrar la expansión de equipos", ex);
        }
    }

    private void clearRuntimeState() {
        listener = null;
        placeholderExpansion = null;
        commandService = null;
        invitationService = null;
        presentationService = null;
        service = null;
    }
}
