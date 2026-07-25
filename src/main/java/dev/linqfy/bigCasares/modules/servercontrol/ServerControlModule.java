package dev.linqfy.bigCasares.modules.servercontrol;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.communication.EmojiAliasService;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import dev.linqfy.bigCasares.modules.moderation.AbuseSignal;
import dev.linqfy.bigCasares.modules.moderation.AuditEvent;
import dev.linqfy.bigCasares.modules.moderation.AuditSeverity;
import dev.linqfy.bigCasares.modules.moderation.AuditSink;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class ServerControlModule implements PluginModule {
    private final BigCasares plugin;
    private final AuditSink audit;
    private final Consumer<AbuseSignal> signalConsumer;
    private final EmojiAliasService configuredEmojiAliases;
    private ServerControlService service;
    private VanishManager vanish;
    private ResistanceManager resistance;
    private ServerControlListener listener;
    private BukkitTask maintenanceTask;
    private Runnable discordPlayerRefresh = () -> { };
    private BiConsumer<Player, String> minecraftChatBridge = (player, message) -> { };
    private BukkitRuntimeRegistrations registrations;
    private RuntimeRegistrationScope compatibilityScope;
    private boolean resistanceStopped;
    private Optional<Boolean> eventEndAccessOverride = Optional.empty();

    public ServerControlModule(BigCasares plugin, AuditSink audit, Consumer<AbuseSignal> signalConsumer) {
        this(plugin, audit, signalConsumer, null);
    }

    public ServerControlModule(
        BigCasares plugin,
        AuditSink audit,
        Consumer<AbuseSignal> signalConsumer,
        EmojiAliasService emojiAliases
    ) {
        this.plugin = plugin;
        this.audit = audit == null ? ignored -> { } : audit;
        this.signalConsumer = signalConsumer == null ? ignored -> { } : signalConsumer;
        this.configuredEmojiAliases = emojiAliases;
    }

    @Override
    public String getId() {
        return "server-control-system";
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
        this.registrations = new BukkitRuntimeRegistrations(plugin, scope);
        scope.register("module-state", this::clearRuntimeState);
        Path statePath = plugin.getDataFolder().toPath()
            .resolve("data").resolve("server-control-system").resolve("state.yml");
        this.service = new ServerControlService(new YamlControlStorage(statePath), Clock.systemUTC());
        this.resistance = new ResistanceManager(service.state().resistanceLevel());
        this.resistanceStopped = false;
        scope.register("resistance-sessions", this::shutdownResistance);
        this.vanish = new VanishManager(
            plugin, plugin.getVanishSessionRegistry(), ServerControlSettings.load(plugin.getConfig()), audit
        );
        scope.register("vanish-sessions", this::shutdownVanish);
        this.vanish.setPlayerListRefresh(this::refreshDiscordPlayers);
        EmojiAliasService emojiAliases = loadEmojiAliases();
        ServerControlMenu menu = new ServerControlMenu(plugin, this);
        this.listener = new ServerControlListener(this, menu, emojiAliases);
        registrations.registerListener("server-control-listener", listener);
        PluginCommand command = plugin.getCommand("servercontrol");
        if (command != null) {
            registrations.bindCommand(
                "server-control-command", command, new ServerControlCommand(menu), command.getTabCompleter());
        }
        PluginCommand adminMeCmd = plugin.getCommand("adminme");
        if (adminMeCmd != null) {
            registrations.bindCommand(
                "admin-me-command", adminMeCmd, new AdminMeCommand(this), null);
        }
        applyPvpToWorlds(service.effectivePvp(Instant.now()));
        service.takeRecoveredExpiration().ifPresent(this::applyPvpTransition);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            resistance.handlePlayer(player);
        }
        vanish.recoverOnlineSessions();
        maintenanceTask = registrations.scheduleRepeating("maintenance-task", this::maintenanceTick, 20L, 20L);
        scope.register("external-callback-bindings", this::resetExternalCallbacks);
        audit.publish(AuditEvent.system(AuditSeverity.INFO, "lifecycle", "server-control-enabled", "Controles del servidor habilitados"));
    }

    @Override
    public void onDisable() {
        if (maintenanceTask != null) {
            maintenanceTask.cancel();
            maintenanceTask = null;
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        shutdownResistance();
        shutdownVanish();
        resetExternalCallbacks();
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
    }

    public ServerControlService service() {
        return service;
    }

    public VanishManager vanish() {
        return vanish;
    }

    public ResistanceManager resistance() {
        return resistance;
    }

    public boolean canBypassRestrictions(Player player) {
        return player.isOp() && vanish.isVanished(player);
    }

    public boolean effectiveEndAccess(Player player) {
        if (eventEndAccessOverride.isPresent() && player.isOp()) {
            return true;
        }
        if (canBypassRestrictions(player)) {
            return true;
        }
        return eventEndAccessOverride.orElseGet(() -> service.state().endAccessEnabled());
    }

    public void setEventEndAccessOverride(Optional<Boolean> override) {
        this.eventEndAccessOverride = override == null ? Optional.empty() : override;
    }

    public void setTimedPvp(Player actor, boolean enabled, Duration duration) {
        PvpTransition transition = service.setTimedPvp(enabled, duration, actor.getName());
        applyPvpTransition(transition);
    }

    public void setPermanentPvp(Player actor, boolean enabled) {
        PvpTransition transition = service.setPermanentPvp(enabled, actor.getName());
        applyPvpTransition(transition);
    }

    public void toggleEndAccess(Player actor) {
        boolean enabled = service.toggleEndAccess();
        actor.sendMessage("§eAcceso al End: " + status(enabled));
        auditControl(actor, "end-access", enabled ? "activado" : "desactivado");
        if (!enabled) {
            expelEndOccupants();
        }
    }

    public void toggleElytraRockets(Player actor) {
        boolean enabled = service.toggleElytraRockets();
        actor.sendMessage("§eCohetes con elytra: " + status(enabled));
        auditControl(actor, "elytra-rockets", enabled ? "activados" : "desactivados");
    }

    public void cycleResistance(Player actor) {
        ResistanceLevel next = service.cycleResistance();
        resistance.changeLevel(next, plugin.getServer().getOnlinePlayers());
        actor.sendMessage("§eResistencia global: §f" + next.name());
        auditControl(actor, "resistance", next.name());
    }

    public void auditControl(Player actor, String action, String value) {
        audit.publish(AuditEvent.player(
            AuditSeverity.INFO, "control", action, actor.getUniqueId(), actor.getName(),
            actor.getName() + " cambió " + action + " a " + value
        ));
    }

    public void signal(AbuseSignal signal) {
        signalConsumer.accept(signal);
    }

    public void refreshDiscordPlayers() {
        runNextTick(discordPlayerRefresh);
    }

    public void setDiscordPlayerRefresh(Runnable refresh) {
        this.discordPlayerRefresh = refresh == null ? () -> { } : refresh;
        if (vanish != null) {
            vanish.setPlayerListRefresh(this::refreshDiscordPlayers);
        }
    }

    public void setMinecraftChatBridge(BiConsumer<Player, String> bridge) {
        this.minecraftChatBridge = bridge == null ? (player, message) -> { } : bridge;
    }

    public void bridgeMinecraftChat(Player player, String message) {
        BukkitRuntimeRegistrations current = registrations;
        BiConsumer<Player, String> bridge = minecraftChatBridge;
        if (current != null) {
            current.guard(() -> bridge.accept(player, message)).run();
        }
    }

    public List<String> visiblePlayerNames() {
        return plugin.getServer().getOnlinePlayers().stream()
            .filter(player -> !vanish.isVanished(player))
            .map(Player::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    public void runNextTick(Runnable runnable) {
        BukkitRuntimeRegistrations current = registrations;
        if (current != null) {
            current.scheduleImmediate("server-control-next-tick", runnable);
        }
    }

    private void maintenanceTick() {
        service.expirePvpOverride(Instant.now()).ifPresent(this::applyPvpTransition);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            resistance.ensureMinimum(player);
        }
    }

    private void applyPvpTransition(PvpTransition transition) {
        applyPvpToWorlds(transition.effectiveState());
        String effective = transition.effectiveState() ? "activado" : "desactivado";
        String message;
        if (transition.expired()) {
            message = "§6[Control] §eFinalizó el cambio temporal de PvP iniciado por §f" + transition.actor()
                + "§e. Se restauró el estado persistente: §f" + effective + "§e.";
        } else if (transition.duration().isPresent()) {
            long minutes = transition.duration().orElseThrow().toMinutes();
            String restoration = transition.baselineState() ? "activado" : "desactivado";
            message = "§6[Control] §f" + transition.actor() + " §edejó el PvP §f" + effective
                + " §epor §f" + minutes + " minutos§e; después volverá a §f" + restoration + "§e.";
        } else {
            message = "§6[Control] §f" + transition.actor() + " §ecambió el PvP permanentemente a §f" + effective + "§e.";
        }
        plugin.getServer().broadcastMessage(message);
        audit.publish(AuditEvent.system(
            AuditSeverity.INFO, "control", transition.expired() ? "pvp-expired" : "pvp-changed", message
        ));
    }

    private void applyPvpToWorlds(boolean enabled) {
        for (World world : plugin.getServer().getWorlds()) {
            world.setPVP(enabled);
        }
    }

    private void expelEndOccupants() {
        Location safeSpawn = findSafeNormalSpawn();
        List<Player> affected = plugin.getServer().getWorlds().stream()
            .filter(world -> world.getEnvironment() == World.Environment.THE_END)
            .flatMap(world -> world.getPlayers().stream())
            .filter(player -> !canBypassRestrictions(player))
            .toList();
        if (affected.isEmpty()) {
            return;
        }
        if (safeSpawn == null) {
            String message = "No existe un mundo normal cargado para expulsar jugadores del End";
            plugin.getLogger().severe(message);
            audit.publish(AuditEvent.system(AuditSeverity.SEVERE, "control", "end-expulsion-failed", message));
            return;
        }
        for (Player player : affected) {
            player.teleport(safeSpawn);
            player.sendMessage("§cEl acceso al End fue desactivado.");
        }
    }

    private Location findSafeNormalSpawn() {
        return plugin.getServer().getWorlds().stream()
            .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
            .findFirst()
            .map(this::findSafeSpawnIn)
            .orElse(null);
    }

    private Location findSafeSpawnIn(World world) {
        Location origin = world.getSpawnLocation();
        for (int radius = 0; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    int blockX = origin.getBlockX() + x;
                    int blockZ = origin.getBlockZ() + z;
                    int y = world.getHighestBlockYAt(blockX, blockZ) + 1;
                    if (y <= world.getMinHeight() || y + 1 >= world.getMaxHeight()) {
                        continue;
                    }
                    Material below = world.getBlockAt(blockX, y - 1, blockZ).getType();
                    if (world.getBlockAt(blockX, y, blockZ).isPassable()
                        && world.getBlockAt(blockX, y + 1, blockZ).isPassable()
                        && below.isSolid() && !isDangerousFloor(below)) {
                        return new Location(world, blockX + 0.5, y, blockZ + 0.5, origin.getYaw(), origin.getPitch());
                    }
                }
            }
        }
        return null;
    }

    private void resetExternalCallbacks() {
        discordPlayerRefresh = () -> { };
        minecraftChatBridge = (player, message) -> { };
    }

    private void shutdownResistance() {
        if (resistanceStopped || resistance == null || plugin == null) {
            return;
        }
        resistanceStopped = true;
        resistance.shutdown(plugin.getServer().getOnlinePlayers());
    }

    private void shutdownVanish() {
        if (vanish == null || plugin == null) {
            return;
        }
        vanish.setPlayerListRefresh(null);
        if (!plugin.isReloadingPluginState()) {
            vanish.clearOnlineSessions();
        }
    }

    private void clearRuntimeState() {
        maintenanceTask = null;
        listener = null;
        registrations = null;
        resistance = null;
        resistanceStopped = true;
        vanish = null;
        service = null;
        eventEndAccessOverride = Optional.empty();
        resetExternalCallbacks();
    }

    private boolean isDangerousFloor(Material material) {
        return material == Material.MAGMA_BLOCK || material == Material.CACTUS
            || material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE
            || material == Material.FIRE || material == Material.SOUL_FIRE;
    }

    private EmojiAliasService loadEmojiAliases() {
        if (configuredEmojiAliases != null) {
            return configuredEmojiAliases;
        }
        var section = plugin.getConfig().getConfigurationSection("emoji-aliases");
        if (section == null) {
            return EmojiAliasService.defaults();
        }
        java.util.Map<String, String> aliases = new java.util.LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String alias = key.startsWith(":") ? key : ":" + key + ":";
            aliases.put(alias, section.getString(key, ""));
        }
        return new EmojiAliasService(aliases);
    }

    private String status(boolean enabled) {
        return enabled ? "§aActivado" : "§cDesactivado";
    }
}
