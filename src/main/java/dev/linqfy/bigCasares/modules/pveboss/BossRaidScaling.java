package dev.linqfy.bigCasares.modules.pveboss;

public record BossRaidScaling(
    double baseHealth,
    double healthPerPlayer,
    int minimumPlayers,
    int maximumPlayers
) {

    public BossRaidScaling {
        if (!Double.isFinite(baseHealth) || baseHealth <= 0.0) {
            throw new IllegalArgumentException("base health must be finite and positive");
        }
        if (!Double.isFinite(healthPerPlayer) || healthPerPlayer < 0.0) {
            throw new IllegalArgumentException("health per player must be finite and nonnegative");
        }
        if (minimumPlayers < 1) {
            throw new IllegalArgumentException("minimum players must be at least one");
        }
        if (maximumPlayers < minimumPlayers) {
            throw new IllegalArgumentException("maximum players must not be below minimum players");
        }
    }

    public int clampedParticipants(int actualParticipants) {
        if (actualParticipants < 0) {
            throw new IllegalArgumentException("actual participants must be nonnegative");
        }
        return Math.clamp(actualParticipants, minimumPlayers, maximumPlayers);
    }

    public double maximumHealth(int actualParticipants) {
        double result = baseHealth + healthPerPlayer * clampedParticipants(actualParticipants);
        if (!Double.isFinite(result)) {
            throw new IllegalStateException("computed maximum health must be finite");
        }
        return result;
    }
}
