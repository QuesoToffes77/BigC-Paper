package dev.linqfy.bigCasares.modules.toffy.eventos;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.dimension.DimensionPlatform;
import dev.linqfy.bigCasares.dimension.DimensionStatus;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.InventoryCustody;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Staff-started events (Lifesteal, KOTH, TNT Tag, Spleef) of the Toffy Complementaries module. An event always runs
 * on a tournament's sign-up list. {@link dev.linqfy.bigCasares.modules.toffy.ToffyModule} creates this object and
 * enables it with its own registrations, so everything here is undone when the module is disabled.
 */
public final class ToffyEvents {
    /** Chat text cases, defined in {@code toffy_mensajes.yml}. */
    public static final String CASE_LOBBY = "evento-lobby";
    public static final String CASE_JOINED = "evento-unido";
    public static final String CASE_LEFT = "evento-salio";
    public static final String CASE_STARTED = "evento-empieza";
    public static final String CASE_WINNER = "evento-ganador";
    public static final String CASE_ELIMINATED = "evento-eliminado";
    public static final String CASE_CANCELLED = "evento-cancelado";
    public static final String CASE_FINISHED = "evento-fin";

    /** Staff permission to start, stop and inspect events. */
    public static final String ADMIN_PERMISSION = ToffyEventRuntime.ADMIN_PERMISSION;
    public static final String PREFIX = ToffyEventRuntime.PREFIX;
    /** Shown by the Toffy commands when the events are not running (failed to start or module off). */
    public static final String UNAVAILABLE_TEXT = "§cLos eventos no estan disponibles.";
    private static final String UNAVAILABLE = UNAVAILABLE_TEXT;

    /**
     * A tournament as the events see it: its name and the players signed up to it. {@code group} is the key of the
     * group the event is limited to, or null for every sign-up.
     */
    public record Roster(String displayName, Set<UUID> players, String group) {
        public Roster {
            players = Set.copyOf(players);
        }

        public Roster(String displayName, Set<UUID> players) {
            this(displayName, players, null);
        }
    }

    /** Renders a chat case for a tournament (override first, then the file). {@code null} means send nothing. */
    @FunctionalInterface
    public interface EventMessages {
        String render(String caseKey, String tournament, Map<String, String> placeholders);
    }

    /** Looks up a tournament by key; empty when it does not exist. */
    @FunctionalInterface
    public interface EventTournaments {
        Optional<Roster> roster(String tournamentKey);

        /** Sign-ups of one group of the tournament only; empty when the tournament or the group does not exist. */
        default Optional<Roster> roster(String tournamentKey, String group) {
            return group == null ? roster(tournamentKey) : Optional.empty();
        }
    }

    private final BigCasares plugin;
    private final Supplier<Optional<DimensionPlatform>> dimensions;
    private final Supplier<Optional<Boolean>> pvpBaseline;
    private final Predicate<UUID> endHuntDeath;
    private final EventMessages messages;
    private final EventTournaments tournaments;
    private volatile ToffyEventRuntime runtime;

    public ToffyEvents(
        BigCasares plugin,
        Supplier<Optional<DimensionPlatform>> dimensions,
        Supplier<Optional<Boolean>> pvpBaseline,
        Predicate<UUID> endHuntDeath,
        EventMessages messages,
        EventTournaments tournaments
    ) {
        this.plugin = plugin;
        this.dimensions = dimensions;
        this.pvpBaseline = pvpBaseline;
        this.endHuntDeath = endHuntDeath;
        this.messages = messages;
        this.tournaments = tournaments;
    }

    /**
     * Starts the event services. The data folder is moved from {@code data/staff-events} first when needed. Everything
     * is registered through {@code registrations} and {@code scope}, so disabling the owner undoes it.
     */
    public void enable(BukkitRuntimeRegistrations registrations, RuntimeRegistrationScope scope,
                       ToffyEventSettings settings) {
        EventDataFolder.Resolution data = EventDataFolder.resolve(plugin.getDataFolder().toPath());
        if (data.conflict() != null) {
            // Checked before anything is registered, so a refused start leaves nothing behind in the scope.
            throw new IllegalStateException(data.conflict());
        }
        if (data.failure() != null) {
            plugin.getLogger().log(Level.SEVERE, "[Eventos] Could not move data/staff-events to "
                + "data/toffy-complementaries/eventos; keeping the old folder so no inventory is lost.", data.failure());
        } else if (data.moved()) {
            plugin.getLogger().info("[Eventos] Moved data/staff-events to data/toffy-complementaries/eventos.");
        }
        Path folder = data.folder();
        EventSnapshotStore store = new EventSnapshotStore(folder.resolve("snapshots.yml"));
        try {
            store.load();
        } catch (IOException failure) {
            // Refuse to run rather than overwrite inventories that are still waiting to be returned.
            throw new IllegalStateException("Could not read event inventories", failure);
        }
        if (settings.arenaDimension()) {
            registerDimension(scope);
        }

        ToffyEventRuntime created = new ToffyEventRuntime(plugin, settings, store, dimensions, this::rewardItem,
            pvpBaseline, endHuntDeath, registrations, messages, tournaments, folder.resolve("rewards-pending"));
        InventoryCustody custody = created.custody();
        var services = plugin.getServer().getServicesManager();
        try {
            services.register(InventoryCustody.class, custody, plugin, ServicePriority.Normal);
            scope.register("toffy-events-custody", () -> services.unregister(InventoryCustody.class, custody));
        } catch (RuntimeException | Error failure) {
            try {
                services.unregister(InventoryCustody.class, custody);
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
        scope.register("toffy-events-runtime", () -> {
            created.shutdown();
            runtime = null;
        });
        runtime = created;
        registrations.registerListener("toffy-events-listener", created);
        registrations.scheduleRepeating("toffy-events-tick", created::tick, 5L, 5L);
        registrations.scheduleRepeating("toffy-events-prepare", created::prepareTick, 1L, 1L);
        registrations.scheduleRepeating("toffy-events-recover", created::retryBlockedRestores, 100L, 100L);
        // Other inventory owners register during module startup; recover only after they are visible.
        registrations.scheduleImmediate("toffy-events-initial-restore", created::restorePendingOnline);
    }

    /** Whether a death is already handled by a running event (keep inventory, no tombstone). */
    public boolean protectsDeath(UUID playerId) {
        ToffyEventRuntime current = runtime;
        return current != null && current.protectsDeath(playerId);
    }

    /** Whether the player is inside the running arena event. */
    public boolean participates(UUID playerId) {
        ToffyEventRuntime current = runtime;
        return current != null && current.participates(playerId);
    }

    /** Settings for the next event; a running event keeps its own until it ends. */
    public void applySettings(ToffyEventSettings settings) {
        ToffyEventRuntime current = runtime;
        if (current != null) {
            current.applySettings(settings);
        }
    }

    /**
     * {@code /toffy evento iniciar}: starts an event on the tournament's sign-up list. The online sign-ups join the
     * lobby; {@code lobbySeconds} overrides the configured countdown for arena events.
     */
    public boolean start(CommandSender sender, ToffyEventType type, String tournamentKey, Integer lobbySeconds) {
        return start(sender, type, tournamentKey, null, lobbySeconds);
    }

    /**
     * Same as {@link #start(CommandSender, ToffyEventType, String, Integer)}, limited to the members of {@code group}
     * when it is not null.
     */
    public boolean start(CommandSender sender, ToffyEventType type, String tournamentKey, String group,
                         Integer lobbySeconds) {
        ToffyEventRuntime current = runtime;
        if (current == null) {
            sender.sendMessage(UNAVAILABLE);
            return false;
        }
        Optional<Roster> roster = tournaments.roster(tournamentKey, group);
        if (roster.isEmpty()) {
            sender.sendMessage(group == null
                ? PREFIX + "§cNo existe el torneo " + tournamentKey + "."
                : PREFIX + "§cNo existe el grupo " + group + " en el torneo " + tournamentKey + ".");
            return false;
        }
        return current.start(sender, type, tournamentKey, roster.get(), lobbySeconds);
    }

    /** Group the running event (in any phase) is limited to, if any. */
    public Optional<String> activeGroup() {
        ToffyEventRuntime current = runtime;
        return current == null ? Optional.empty() : current.activeGroup();
    }

    /** Whether a new event may start now: events enabled and none running. */
    public boolean startable() {
        ToffyEventRuntime current = runtime;
        return current != null && current.startable();
    }

    /** Tournament of the running event, if any. */
    public Optional<String> activeTournament() {
        ToffyEventRuntime current = runtime;
        return current == null ? Optional.empty() : current.activeTournament();
    }

    /** Group the running event is limited to, if any (its lobby players must be members of it). */
    public Optional<String> lobbyGroup() {
        ToffyEventRuntime current = runtime;
        return current == null ? Optional.empty() : current.lobbyGroup();
    }

    /** Tournament whose event is waiting in its lobby, if any. */
    public Optional<String> lobbyTournament() {
        ToffyEventRuntime current = runtime;
        return current == null ? Optional.empty() : current.lobbyTournament();
    }

    /** {@code /toffy evento detener}. */
    public void stop(CommandSender sender) {
        ToffyEventRuntime current = runtime;
        if (current == null) {
            sender.sendMessage(UNAVAILABLE);
            return;
        }
        current.stop(sender);
    }

    /** Lines for {@code /toffy evento estado} and {@code /anotar estado}. */
    public List<String> status() {
        ToffyEventRuntime current = runtime;
        return current == null ? List.of(PREFIX + "§7No hay ningún evento en curso.") : current.status();
    }

    /** {@code /anotar <torneo>} while the lobby of that tournament's event is open. */
    public void joinLobby(Player player, String tournamentKey) {
        ToffyEventRuntime current = runtime;
        if (current != null) {
            current.joinLobby(player, tournamentKey);
        }
    }

    /**
     * Takes the player out of the running event of that tournament ({@code /anotar salir} or {@code /toffy torneo
     * sacar}). Works for offline players too. False when the player was not in that event.
     */
    public boolean withdraw(UUID playerId, String tournamentKey) {
        ToffyEventRuntime current = runtime;
        return current != null && current.withdraw(playerId, tournamentKey);
    }

    private void registerDimension(RuntimeRegistrationScope scope) {
        Optional<DimensionPlatform> platform = dimensions.get();
        if (platform.isEmpty()) {
            plugin.getLogger().warning("[Eventos] dimension-framework is disabled; arenas need fallback-arena-world.");
            return;
        }
        try {
            DimensionStatus status = platform.get().register(EventArenaDimension.create());
            plugin.getLogger().info("[Eventos] Registered " + EventArenaDimension.KEY + ": " + status.state()
                + (status.message().isBlank() ? "" : " (" + status.message() + ")"));
            scope.register("toffy-events-dimension", () -> platform.get().unregister(EventArenaDimension.KEY));
        } catch (RuntimeException failure) {
            plugin.getLogger().log(Level.SEVERE, "[Eventos] Could not register the event dimension", failure);
        }
    }

    private Optional<ItemStack> rewardItem(RewardItem reward) {
        if (reward.vanilla()) {
            Material material = Material.matchMaterial(reward.id());
            return material == null || !material.isItem()
                ? Optional.empty()
                : Optional.of(new ItemStack(material, reward.amount()));
        }
        var registry = plugin.getCustomItemRegistry();
        return registry == null
            ? Optional.empty()
            : registry.findById(reward.id()).map(item -> item.createItemStack(reward.amount()));
    }
}
