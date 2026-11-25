package dev.linqfy.bigCasares.modules.toffy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    private final ToffyTournamentStorage storage;
    private final Map<String, ToffyTournament> tournaments = new LinkedHashMap<>();

    ToffyTournamentService(ToffyTournamentStorage storage, Collection<ToffyTournament> initial) {
        this.storage = storage;
        initial.forEach(tournament -> tournaments.put(tournament.key(), tournament));
    }

    static ToffyTournamentService load(ToffyTournamentStorage storage) throws IOException {
        return new ToffyTournamentService(storage, storage.load());
    }

    /**
     * @throws IllegalArgumentException invalid name
     * @throws IllegalStateException    the tournament already exists
     */
    ToffyTournament create(String name) {
        String key = ToffyTournament.keyOf(name);
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

    private void save() {
        try {
            storage.save(tournaments.values());
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }
}
