package dev.linqfy.bigCasares.modules.endevent;

import java.time.Instant;
import java.util.Optional;

public record EndEventParticipantSnapshot(
    boolean eliminated,
    boolean online,
    int disconnects,
    Optional<Instant> combatUntil
) {

    public EndEventParticipantSnapshot {
        combatUntil = combatUntil == null ? Optional.empty() : combatUntil;
    }

    public static EndEventParticipantSnapshot active() {
        return new EndEventParticipantSnapshot(false, true, 0, Optional.empty());
    }

    public EndEventParticipantSnapshot withEliminated(boolean value) {
        return new EndEventParticipantSnapshot(value, online, disconnects, combatUntil);
    }

    public EndEventParticipantSnapshot withOnline(boolean value) {
        return new EndEventParticipantSnapshot(eliminated, value, disconnects, combatUntil);
    }

    public EndEventParticipantSnapshot withDisconnects(int value) {
        return new EndEventParticipantSnapshot(eliminated, online, value, combatUntil);
    }

    public EndEventParticipantSnapshot withCombatUntil(Optional<Instant> value) {
        return new EndEventParticipantSnapshot(eliminated, online, disconnects, value);
    }
}
