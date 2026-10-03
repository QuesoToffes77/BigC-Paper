package dev.linqfy.bigCasares.modules.acidrain;

import java.util.Random;

/**
 * Pure drop-roll logic for the toxic mob {@code nitric_acid} loot. Kept free
 * of Bukkit/adventure dependencies so it is unit-testable without a server.
 */
final class AcidRainMobDrops {

    private AcidRainMobDrops() {
    }

    /**
     * Disabled settings, a zero chance or a zero max amount yield no drop;
     * otherwise the amount is uniform between min and max.
     */
    static int rollAmount(AcidRainMobDropSettings settings, Random random) {
        if (settings == null || !settings.enabled() || settings.maxAmount() <= 0
            || random.nextInt(100) >= settings.chancePercent()) {
            return 0;
        }
        int amount = settings.minAmount();
        int spread = settings.maxAmount() - settings.minAmount();
        if (spread > 0) {
            amount += random.nextInt(spread + 1);
        }
        return Math.max(0, amount);
    }
}
