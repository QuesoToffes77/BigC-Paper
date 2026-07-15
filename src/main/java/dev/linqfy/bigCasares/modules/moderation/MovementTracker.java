package dev.linqfy.bigCasares.modules.moderation;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MovementTracker {
    private final Map<UUID, Integer> consecutiveFastSamples = new HashMap<>();
    private final Map<UUID, Instant> exemptUntil = new HashMap<>();

    public void exempt(UUID playerId, Instant until) {
        exemptUntil.merge(playerId, until, (left, right) -> left.isAfter(right) ? left : right);
        consecutiveFastSamples.remove(playerId);
    }

    public Optional<AbuseSignal> sample(
        Instant now,
        UUID playerId,
        String playerName,
        double horizontalDistance,
        double upwardDistance,
        double expectedSpeed,
        boolean exempt
    ) {
        if (exempt || now.isBefore(exemptUntil.getOrDefault(playerId, Instant.MIN))) {
            consecutiveFastSamples.remove(playerId);
            return Optional.empty();
        }
        if (upwardDistance > 1.2) {
            consecutiveFastSamples.remove(playerId);
            return Optional.of(new AbuseSignal(
                now, playerId, playerName, "upward-movement", 30,
                "Ascenso de " + format(upwardDistance) + " bloques por muestra"
            ));
        }
        if (horizontalDistance > expectedSpeed * 1.8) {
            int samples = consecutiveFastSamples.merge(playerId, 1, Integer::sum);
            if (samples >= 3) {
                consecutiveFastSamples.remove(playerId);
                return Optional.of(new AbuseSignal(
                    now, playerId, playerName, "movement-speed", 25,
                    "3 muestras a más de 1.8x; observado=" + format(horizontalDistance)
                        + ", esperado=" + format(expectedSpeed)
                ));
            }
        } else {
            consecutiveFastSamples.remove(playerId);
        }
        return Optional.empty();
    }

    public void forget(UUID playerId) {
        consecutiveFastSamples.remove(playerId);
        exemptUntil.remove(playerId);
    }

    private String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
