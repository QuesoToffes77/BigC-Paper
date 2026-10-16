package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Objects;

public record DonPollosBattleSettings(
    int hitsToWin,
    int lives,
    int startWindowTicks,
    int minWindowTicks,
    int pauseTicks,
    DonPollosItem reward,
    double maxDistance,
    int minHumanWindowTicks,
    int rewardCooldownMinutes
) {

    public DonPollosBattleSettings {
        Objects.requireNonNull(reward, "reward");
        if (hitsToWin < 1 || lives < 1) {
            throw new IllegalArgumentException("salsero.pvp needs hits-to-win and lives >= 1");
        }
        if (minWindowTicks < 5 || startWindowTicks < minWindowTicks) {
            throw new IllegalArgumentException("salsero.pvp needs 5 <= min-window-ticks <= window-ticks");
        }
        if (pauseTicks < 0 || maxDistance <= 0) {
            throw new IllegalArgumentException("salsero.pvp pause-ticks and max-distance must be positive");
        }
        if (minHumanWindowTicks > startWindowTicks) {
            throw new IllegalArgumentException("salsero.pvp needs min-human-window-ticks <= window-ticks");
        }
        if (minHumanWindowTicks < 1 || rewardCooldownMinutes < 0) {
            throw new IllegalArgumentException(
                "salsero.pvp needs min-human-window-ticks >= 1 and reward-cooldown-minutes >= 0");
        }
    }

    /** Cuantas victorias pagan el premio completo; desde la siguiente el Salsero da 1. */
    public static final int FULL_REWARD_WINS = 2;

    private static final long MILLIS_PER_MINUTE = 60_000L;

    /** Premio de la victoria numero winNumber de ese jugador (1 = la primera). */
    public int rewardAmount(int winNumber) {
        return winNumber <= FULL_REWARD_WINS ? reward.amount() : Math.min(1, reward.amount());
    }

    /**
     * Minutos (redondeados hacia arriba) que faltan para que el Salsero vuelva a pagar, o 0 si ya puede pagar.
     * lastRewardMillis <= 0 significa que nunca le pago.
     */
    public long rewardCooldownRemainingMinutes(long lastRewardMillis, long nowMillis) {
        if (rewardCooldownMinutes <= 0 || lastRewardMillis <= 0) {
            return 0;
        }
        long elapsed = nowMillis - lastRewardMillis;
        if (elapsed < 0) {
            // reloj movido hacia atras: se cuenta como recien pagado
            elapsed = 0;
        }
        long remaining = rewardCooldownMinutes * MILLIS_PER_MINUTE - elapsed;
        return remaining <= 0 ? 0 : (remaining + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE;
    }

    public static DonPollosBattleSettings defaults() {
        return new DonPollosBattleSettings(12, 3, 50, 22, 12, new DonPollosItem("DIAMOND", 2), 16.0, 6, 30);
    }
}
