package dev.linqfy.bigCasares.modules.toffy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Tournaments in memory (main thread). Every change is saved through the storage; when saving fails the change stays
 * in memory and an {@link UncheckedIOException} is thrown.
 */
final class ToffyTournamentService {
    enum RegisterResult {
        REGISTERED, ALREADY_REGISTERED, CLOSED, UNKNOWN_TOURNAMENT
    }

    enum Change {
        CHANGED, UNCHANGED, UNKNOWN
    }

    record PendingReturn(String tournament, ToffyStoredLocation location) {
    }

    /** A reminder due for the scheduled teleport of {@code tournament}, {@code minutes} before it. */
    record Warning(String tournament, int minutes) {
    }

    /** Names that /anotar uses for its own subcommands, so a tournament cannot take them. */
    static final Set<String> RESERVED_NAMES = Set.of("salir", "estado");
    /** Minutes before a scheduled teleport when the sign-ups are warned. */
    static final int[] WARNING_MINUTES = {5, 1};
    /** A schedule more than this late when the file is loaded is dropped (the server was off). */
    static final long MISSED_AFTER_MILLIS = 5 * 60_000L;
    private static final long MINUTE_MILLIS = 60_000L;

    private final ToffyTournamentStorage storage;
    private final LongSupplier clock;
    private final Consumer<String> log;
    private final Map<String, ToffyTournament> tournaments = new LinkedHashMap<>();

    ToffyTournamentService(ToffyTournamentStorage storage, Collection<ToffyTournament> initial) {
        this(storage, initial, System::currentTimeMillis, message -> { });
    }

    /**
     * @param clock current epoch millis
     * @param log   receives the warnings about what was dropped or could not be saved while loading
     */
    ToffyTournamentService(ToffyTournamentStorage storage, Collection<ToffyTournament> initial, LongSupplier clock,
                           Consumer<String> log) {
        this.storage = storage;
        this.clock = clock;
        this.log = log;
        initial.forEach(tournament -> tournaments.put(tournament.key(), tournament));
        restoreSchedules();
    }

    static ToffyTournamentService load(ToffyTournamentStorage storage) throws IOException {
        return load(storage, message -> { });
    }

    static ToffyTournamentService load(ToffyTournamentStorage storage, Consumer<String> log) throws IOException {
        return new ToffyTournamentService(storage, storage.load(), System::currentTimeMillis, log);
    }

    /** Current epoch millis of the injected clock. */
    long now() {
        return clock.getAsLong();
    }

    /**
     * @throws IllegalArgumentException invalid name or a reserved one ("salir", "estado")
     * @throws IllegalStateException    the tournament already exists
     */
    ToffyTournament create(String name) {
        String key = ToffyTournament.keyOf(name);
        if (RESERVED_NAMES.contains(key)) {
            throw new IllegalArgumentException("El nombre " + key + " esta reservado para /anotar; elegi otro.");
        }
        if (tournaments.containsKey(key)) {
            throw new IllegalStateException("Ya existe el torneo " + tournaments.get(key).displayName() + ".");
        }
        ToffyTournament tournament = new ToffyTournament(key, name.trim());
        tournaments.put(key, tournament);
        save();
        return tournament;
    }

    Optional<ToffyTournament> find(String name) {
        try {
            return Optional.ofNullable(tournaments.get(ToffyTournament.keyOf(name)));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    List<ToffyTournament> list() {
        return List.copyOf(tournaments.values());
    }

    List<String> keys() {
        return List.copyOf(tournaments.keySet());
    }

    /** Deleting a tournament also deletes its place and its schedule. */
    boolean delete(String name) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty()) {
            return false;
        }
        tournaments.remove(found.get().key());
        save();
        return true;
    }

    Change close(String name) {
        return setOpen(name, false);
    }

    Change open(String name) {
        return setOpen(name, true);
    }

    private Change setOpen(String name, boolean open) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty()) {
            return Change.UNKNOWN;
        }
        if (found.get().open() == open) {
            return Change.UNCHANGED;
        }
        found.get().setOpen(open);
        save();
        return Change.CHANGED;
    }

    RegisterResult register(String name, UUID playerId, String playerName) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty()) {
            return RegisterResult.UNKNOWN_TOURNAMENT;
        }
        ToffyTournament tournament = found.get();
        if (tournament.participants().containsKey(playerId)) {
            return RegisterResult.ALREADY_REGISTERED;
        }
        if (!tournament.open()) {
            return RegisterResult.CLOSED;
        }
        tournament.putParticipant(playerId, playerName);
        save();
        return RegisterResult.REGISTERED;
    }

    boolean removeParticipant(String name, UUID playerId) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || !found.get().removeParticipant(playerId)) {
            return false;
        }
        save();
        return true;
    }

    /**
     * Saves where the player was. Keeps the first saved location if there is one so teleporting twice still returns
     * the player to the original place.
     *
     * @return true when the location was saved, false when an earlier one was kept or the tournament is unknown
     */
    boolean recordDeparture(String name, UUID playerId, ToffyStoredLocation location) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || found.get().returns().containsKey(playerId)) {
            return false;
        }
        found.get().putReturn(playerId, location);
        save();
        return true;
    }

    int savedPlaces(String name) {
        return find(name).map(tournament -> tournament.returns().size()).orElse(0);
    }

    Optional<ToffyStoredLocation> savedLocation(String name, UUID playerId) {
        return find(name).map(tournament -> tournament.returns().get(playerId));
    }

    /**
     * Forgets the saved location and any pending return of the player.
     */
    boolean clearReturn(String name, UUID playerId) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty()) {
            return false;
        }
        boolean changed = found.get().removeReturn(playerId);
        changed |= found.get().removePending(playerId);
        if (changed) {
            save();
        }
        return changed;
    }

    /**
     * Saved location of the player, removed from the tournament.
     */
    Optional<ToffyStoredLocation> takeReturn(String name, UUID playerId) {
        Optional<ToffyStoredLocation> location = savedLocation(name, playerId);
        if (location.isPresent()) {
            clearReturn(name, playerId);
        }
        return location;
    }

    boolean markPending(String name, UUID playerId) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || !found.get().returns().containsKey(playerId) || !found.get().addPending(playerId)) {
            return false;
        }
        save();
        return true;
    }

    boolean isPending(String name, UUID playerId) {
        return find(name).map(tournament -> tournament.pending().contains(playerId)).orElse(false);
    }

    /**
     * Returns still owed to the player, in tournament order. A pending mark without a saved location is dropped.
     * Nothing is cleared here: call {@link #clearReturn} once the teleport worked.
     */
    List<PendingReturn> pendingReturns(UUID playerId) {
        List<PendingReturn> result = new ArrayList<>();
        boolean dropped = false;
        for (ToffyTournament tournament : tournaments.values()) {
            if (!tournament.pending().contains(playerId)) {
                continue;
            }
            ToffyStoredLocation location = tournament.returns().get(playerId);
            if (location == null) {
                tournament.removePending(playerId);
                dropped = true;
            } else {
                result.add(new PendingReturn(tournament.key(), location));
            }
        }
        if (dropped) {
            save();
        }
        return result;
    }

    boolean setMessage(String name, String caseKey, String text) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty()) {
            return false;
        }
        found.get().putMessage(caseKey, text);
        save();
        return true;
    }

    /**
     * @return true when an override existed and was removed
     */
    boolean resetMessage(String name, String caseKey) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || !found.get().removeMessage(caseKey)) {
            return false;
        }
        save();
        return true;
    }

    /**
     * Sets the fixed destination of the tournament. Any schedule stays as it is.
     *
     * @return false when the tournament does not exist
     */
    boolean setPlace(String name, ToffyStoredLocation place) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty()) {
            return false;
        }
        found.get().setPlace(place);
        save();
        return true;
    }

    /**
     * Removes the fixed destination. A schedule needs a place, so it is cancelled too.
     *
     * @return false when the tournament does not exist or had no place
     */
    boolean clearPlace(String name) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || found.get().place() == null) {
            return false;
        }
        found.get().setPlace(null);
        found.get().setScheduledAt(null);
        save();
        return true;
    }

    /**
     * Schedules the teleport to the place at {@code at} (epoch millis). A schedule that is already there is replaced.
     * Warnings that would have been sent before {@code at} are marked as passed, so they are not sent late.
     *
     * @return the schedule that was replaced, if any
     * @throws IllegalArgumentException the tournament does not exist
     * @throws IllegalStateException    the tournament has no place
     */
    Optional<Long> schedule(String name, long at) {
        ToffyTournament tournament = require(name);
        if (tournament.place() == null) {
            throw new IllegalStateException("El torneo no tiene lugar. Usa /toffy torneo lugar " + tournament.key());
        }
        Long previous = tournament.scheduledAt();
        tournament.setScheduledAt(at);
        markPassedWarnings(tournament, at, now());
        save();
        return Optional.ofNullable(previous);
    }

    /** @return true when a schedule existed and was removed */
    boolean cancelSchedule(String name) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || found.get().scheduledAt() == null) {
            return false;
        }
        found.get().setScheduledAt(null);
        save();
        return true;
    }

    /**
     * Tournaments whose schedule is due at {@code now} (epoch millis at or before it), in tournament order.
     */
    List<String> dueSchedules(long now) {
        List<String> due = new ArrayList<>();
        for (ToffyTournament tournament : tournaments.values()) {
            Long at = tournament.scheduledAt();
            if (at != null && at <= now) {
                due.add(tournament.key());
            }
        }
        return due;
    }

    /**
     * Warnings to send at {@code now}: each minute mark of {@link #WARNING_MINUTES} that has started, has not been
     * sent yet and whose schedule has not passed.
     */
    List<Warning> dueWarnings(long now) {
        List<Warning> due = new ArrayList<>();
        for (ToffyTournament tournament : tournaments.values()) {
            Long at = tournament.scheduledAt();
            if (at == null || now >= at) {
                continue;
            }
            for (int minutes : WARNING_MINUTES) {
                if (!tournament.isAnnounced(minutes) && now >= at - minutes * MINUTE_MILLIS) {
                    due.add(new Warning(tournament.key(), minutes));
                }
            }
        }
        return due;
    }

    /** Records that the warning was sent, so it is not sent again. */
    void markWarned(String name, int minutes) {
        find(name).ifPresent(tournament -> tournament.markAnnounced(minutes));
    }

    /**
     * The teleport of the tournament ran (or could not run): clears its schedule.
     *
     * @return true when there was a schedule to clear
     */
    boolean markFired(String name) {
        Optional<ToffyTournament> found = find(name);
        if (found.isEmpty() || found.get().scheduledAt() == null) {
            return false;
        }
        found.get().setScheduledAt(null);
        save();
        return true;
    }

    private ToffyTournament require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("No existe el torneo " + name + "."));
    }

    /** Schedules loaded from the file: the ones more than five minutes late are dropped, the rest fire normally. */
    private void restoreSchedules() {
        long now = now();
        ZoneId zone = ZoneId.systemDefault();
        boolean dropped = false;
        for (ToffyTournament tournament : tournaments.values()) {
            Long at = tournament.scheduledAt();
            if (at == null) {
                continue;
            }
            if (at < now - MISSED_AFTER_MILLIS) {
                log.accept("Teletransporte programado del torneo " + tournament.displayName() + " para "
                    + ToffyTournamentTime.dateTime(at, zone) + " descartado: el servidor estaba apagado y paso el "
                    + "horario.");
                tournament.setScheduledAt(null);
                dropped = true;
            } else {
                markPassedWarnings(tournament, at, now);
            }
        }
        if (dropped) {
            try {
                save();
            } catch (UncheckedIOException failure) {
                log.accept("No se pudo guardar torneos.yml sin los teletransportes descartados: "
                    + failure.getCause().getMessage());
            }
        }
    }

    private static void markPassedWarnings(ToffyTournament tournament, long at, long now) {
        for (int minutes : WARNING_MINUTES) {
            if (at - minutes * MINUTE_MILLIS <= now) {
                tournament.markAnnounced(minutes);
            }
        }
    }

    private void save() {
        try {
            storage.save(tournaments.values());
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
