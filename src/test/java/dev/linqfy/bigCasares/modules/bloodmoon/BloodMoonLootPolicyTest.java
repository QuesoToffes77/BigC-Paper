package dev.linqfy.bigCasares.modules.bloodmoon;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonLootPolicyTest {

    @Test
    void addsOneModerateBonusWithoutReplacingVanillaLoot() {
        BloodMoonLootSettings settings = new BloodMoonLootSettings(
            true, 0.15, 1, 3, 2, 6, List.of(Material.IRON_NUGGET, Material.EMERALD));
        BloodMoonLootRoll roll = new BloodMoonLootPolicy(settings, fixedRandom(0.01, 0))
            .roll(true).orElseThrow();

        assertEquals(Material.IRON_NUGGET, roll.material());
        assertEquals(1, roll.amount());
        assertEquals(2, roll.experience());
    }

    @Test
    void ineligibleOrFailedChanceProducesNoBonus() {
        BloodMoonLootSettings settings = BloodMoonLootSettings.defaults();
        BloodMoonLootPolicy policy = new BloodMoonLootPolicy(settings, fixedRandom(0.99, 0));

        assertTrue(policy.roll(false).isEmpty());
        assertTrue(policy.roll(true).isEmpty());
    }

    private static Random fixedRandom(double chance, int value) {
        return new Random() {
            @Override public double nextDouble() { return chance; }
            @Override public int nextInt(int bound) { return Math.min(value, bound - 1); }
        };
    }
}
