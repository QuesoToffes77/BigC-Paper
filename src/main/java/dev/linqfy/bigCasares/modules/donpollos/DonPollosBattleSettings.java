package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Objects;

public record DonPollosBattleSettings(
    int hitsToWin,
    int lives,
    int startWindowTicks,
    int minWindowTicks,
    int pauseTicks,
    DonPollosItem reward,
    double maxDistance
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
    }

    /** Cuantas victorias pagan el premio completo; desde la siguiente el Salsero da 1. */
    public static final int FULL_REWARD_WINS = 2;

    /** Premio de la victoria numero winNumber de ese jugador (1 = la primera). */
    public int rewardAmount(int winNumber) {
        return winNumber <= FULL_REWARD_WINS ? reward.amount() : Math.min(1, reward.amount());
    }

    public static DonPollosBattleSettings defaults() {
        return new DonPollosBattleSettings(12, 3, 50, 22, 12, new DonPollosItem("DIAMOND", 2), 16.0);
    }
}
