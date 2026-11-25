package dev.linqfy.bigCasares.modules.toffy;

import java.util.Collections;
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

    void setOpen(boolean open) {
        this.open = open;
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
