package dev.linqfy.bigCasares.modules.toffy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * One tournament. Mutable and main-thread only; {@link ToffyTournamentService} is the only code that should change
 * it after loading.
 */
final class ToffyTournament {
    static final int MAX_NAME_LENGTH = 32;
    private static final Pattern VALID_KEY = Pattern.compile("[a-z0-9_-]+");

    private final String key;
    private final String displayName;
    private boolean open = true;
    private final Map<UUID, String> participants = new LinkedHashMap<>();
    private final Map<String, String> messages = new LinkedHashMap<>();
    private final Map<UUID, ToffyStoredLocation> returns = new LinkedHashMap<>();
    private final Set<UUID> pending = new LinkedHashSet<>();
    private final List<ToffyStoredLocation> places = new ArrayList<>();
    private Long scheduledAt;
    /** Minutes-before warnings already sent (or passed) for the current schedule. Not saved. */
    private final Set<Integer> announced = new HashSet<>();
    private Double minLevel;
    private Double maxLevel;
    private Integer capacity;
    private Integer minPlayers;
    private Long closeAt;
    private String eventType;
    private Integer eventLobby;
    private Long eventAt;
    /** Minutes-before event warnings already sent (or passed). Not saved. */
    private final Set<Integer> eventAnnounced = new HashSet<>();

    ToffyTournament(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    /**
     * Lowercase key of a typed name.
     *
     * @throws IllegalArgumentException when the name is empty, too long or has characters other than a-z 0-9 _ -
     */
    static String keyOf(String name) {
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || key.length() > MAX_NAME_LENGTH || !VALID_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("El nombre del torneo solo puede tener letras sin acentos, numeros, "
                + "_ y - (maximo " + MAX_NAME_LENGTH + " caracteres).");
        }
        return key;
    }

    String key() {
        return key;
    }

    String displayName() {
        return displayName;
    }

    boolean open() {
        return open;
    }

    Map<UUID, String> participants() {
        return Collections.unmodifiableMap(participants);
    }

    Map<String, String> messages() {
        return Collections.unmodifiableMap(messages);
    }

    String message(String caseKey) {
        return messages.get(caseKey);
    }

    Map<UUID, ToffyStoredLocation> returns() {
        return Collections.unmodifiableMap(returns);
    }

    Set<UUID> pending() {
        return Collections.unmodifiableSet(pending);
    }

    /** Fixed destinations of the teleports, in the order they were added. Empty when there are none. */
    List<ToffyStoredLocation> places() {
        return Collections.unmodifiableList(places);
    }

    Double minLevel() {
        return minLevel;
    }

    Double maxLevel() {
        return maxLevel;
    }

    /** Whether a player with this Toffy level may sign up or be teleported (no bound means no limit). */
    boolean allowsLevel(double level) {
        return (minLevel == null || level >= minLevel) && (maxLevel == null || level <= maxLevel);
    }

    Integer capacity() {
        return capacity;
    }

    Integer minPlayers() {
        return minPlayers;
    }

    /** Epoch millis of the scheduled sign-up close, or null. */
    Long closeAt() {
        return closeAt;
    }

    /** Type id of the scheduled event, or null. */
    String eventType() {
        return eventType;
    }

    Integer eventLobby() {
        return eventLobby;
    }

    /** Epoch millis of the scheduled event, or null. */
    Long eventAt() {
        return eventAt;
    }

    boolean isEventAnnounced(int minutes) {
        return eventAnnounced.contains(minutes);
    }

    void markEventAnnounced(int minutes) {
        eventAnnounced.add(minutes);
    }

    void setLevelRange(Double min, Double max) {
        this.minLevel = min;
        this.maxLevel = max;
    }

    void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    void setMinPlayers(Integer minPlayers) {
        this.minPlayers = minPlayers;
    }

    void setCloseAt(Long at) {
        this.closeAt = at;
    }

    /** Sets or clears (all null) the scheduled event. */
    void setEvent(String type, Integer lobby, Long at) {
        this.eventType = type;
        this.eventLobby = lobby;
        this.eventAt = at;
        eventAnnounced.clear();
    }

    /** Epoch millis of the scheduled teleport, or null when nothing is scheduled. */
    Long scheduledAt() {
        return scheduledAt;
    }

    boolean isAnnounced(int minutes) {
        return announced.contains(minutes);
    }

    void markAnnounced(int minutes) {
        announced.add(minutes);
    }

    /** Closing the sign-ups also drops a scheduled close: it would have nothing left to do. */
    void setOpen(boolean open) {
        this.open = open;
        if (!open) {
            closeAt = null;
        }
    }

    void setPlaces(List<ToffyStoredLocation> newPlaces) {
        places.clear();
        places.addAll(newPlaces);
    }

    void addPlace(ToffyStoredLocation place) {
        places.add(place);
    }

    /** Sets or clears the schedule. Resets the warnings, so the caller marks the ones that already passed. */
    void setScheduledAt(Long at) {
        this.scheduledAt = at;
        announced.clear();
    }

    void putParticipant(UUID id, String name) {
        participants.put(id, name);
    }

    boolean removeParticipant(UUID id) {
        return participants.remove(id) != null;
    }

    void putMessage(String caseKey, String text) {
        messages.put(caseKey, text);
    }

    boolean removeMessage(String caseKey) {
        return messages.remove(caseKey) != null;
    }

    void putReturn(UUID id, ToffyStoredLocation location) {
        returns.put(id, location);
    }

    boolean removeReturn(UUID id) {
        return returns.remove(id) != null;
    }

    boolean addPending(UUID id) {
        return pending.add(id);
    }

    boolean removePending(UUID id) {
        return pending.remove(id);
    }
}
