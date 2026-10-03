package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropQualitySystemTest {

    @Test
    void qualityChancesSumToOne() {
        assertEquals(1.0, AirdropQualitySettings.defaults().totalChance(), 0.000_001);
    }

    @Test
    void configuredBaseChancesMatchTheDesign() {
        Map<AirdropQuality, Double> chances = AirdropQualitySettings.defaults().chances();

        assertEquals(0.39, chances.get(AirdropQuality.COMMON));
        assertEquals(0.30, chances.get(AirdropQuality.RARE));
        assertEquals(0.20, chances.get(AirdropQuality.EPIC));
        assertEquals(0.10, chances.get(AirdropQuality.LEGENDARY));
        assertEquals(0.01, chances.get(AirdropQuality.GHISTIC));
    }

    @Test
    void qualityRollAlwaysReturnsExactlyOneQuality() {
        AirdropQualitySelector selector = new AirdropQualitySelector(
            AirdropQualitySettings.defaults().chances());
        Random random = new Random(8217L);

        for (int roll = 0; roll < 100_000; roll++) {
            assertTrue(selector.roll(random) instanceof AirdropQuality);
        }
    }

    @Test
    void oneHundredThousandRollsFollowConfiguredDistribution() {
        AirdropQualitySelector selector = new AirdropQualitySelector(
            AirdropQualitySettings.defaults().chances());
        EnumMap<AirdropQuality, Integer> counts = new EnumMap<>(AirdropQuality.class);
        Random random = new Random(8217L);

        for (int roll = 0; roll < 100_000; roll++) {
            counts.merge(selector.roll(random), 1, Integer::sum);
        }

        for (Map.Entry<AirdropQuality, Double> expected
            : AirdropQualitySettings.defaults().chances().entrySet()) {
            double actual = counts.getOrDefault(expected.getKey(), 0) / 100_000.0;
            assertEquals(expected.getValue(), actual, 0.006,
                () -> expected.getKey() + " distribution was " + actual);
        }
    }

    @Test
    void eachQualityIsHarderThanThePreviousOne() {
        AirdropQualitySettings settings = AirdropQualitySettings.defaults();

        for (int rank = 1; rank < AirdropQuality.values().length; rank++) {
            AirdropQualityProfile previous = settings.profile(AirdropQuality.values()[rank - 1]);
            AirdropQualityProfile current = settings.profile(AirdropQuality.values()[rank]);
            assertTrue(current.guardCount() > previous.guardCount());
            assertTrue(current.healthMultiplier() > previous.healthMultiplier());
            assertTrue(current.damageMultiplier() > previous.damageMultiplier());
            assertTrue(current.equipmentLevel() > previous.equipmentLevel());
            assertTrue(current.lootRolls() > previous.lootRolls());
        }
    }

    @Test
    void lootCanPromoteThroughConsecutiveSuccessfulRolls() {
        AirdropQualityPromoter promoter = new AirdropQualityPromoter(
            AirdropQualitySettings.defaults().promotionChances());

        assertEquals(AirdropQuality.GHISTIC,
            promoter.promote(AirdropQuality.COMMON,
                new SequenceRandom(0.01, 0.01, 0.01, 0.01)));
    }

    @Test
    void failedPromotionNeverDemotesAndGhisticNeverPromotesFurther() {
        AirdropQualityPromoter promoter = new AirdropQualityPromoter(
            AirdropQualitySettings.defaults().promotionChances());

        assertEquals(AirdropQuality.EPIC,
            promoter.promote(AirdropQuality.EPIC, new SequenceRandom(0.99)));
        assertEquals(AirdropQuality.GHISTIC,
            promoter.promote(AirdropQuality.GHISTIC, new SequenceRandom(0.0)));
    }

    private static final class SequenceRandom extends Random {
        private final double[] values;
        private int index;

        private SequenceRandom(double... values) {
            this.values = values;
        }

        @Override
        public double nextDouble() {
            return values[Math.min(index++, values.length - 1)];
        }
    }
}
