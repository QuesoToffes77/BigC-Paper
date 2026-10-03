package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropGuardDifficultyTest {

    @Test
    void guardCountAndEquipmentScaleWithEveryQuality() {
        AirdropQualitySettings settings = AirdropQualitySettings.defaults();
        int previousCount = 0;
        int previousEquipment = 0;

        for (AirdropQuality quality : AirdropQuality.values()) {
            AirdropQualityProfile profile = settings.profile(quality);
            AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(
                AirdropMobSettings.defaults(), quality, profile, new Random(quality.ordinal()));
            AirdropGuardLoadout loadout = AirdropGuardLoadout.forProfile(profile);
            assertEquals(profile.guardCount(), plan.totalEntities());
            assertTrue(profile.guardCount() > previousCount);
            assertTrue(loadout.equipmentScore() > previousEquipment);
            previousCount = profile.guardCount();
            previousEquipment = loadout.equipmentScore();
        }
    }

    @Test
    void overpoweredGuardEquipmentDoesNotDropByDefault() {
        AirdropQualitySettings settings = AirdropQualitySettings.defaults();

        assertEquals(0.0, settings.profile(AirdropQuality.GHISTIC).equipmentDropChance());
        assertTrue(settings.profile(AirdropQuality.LEGENDARY).equipmentDropChance() <= 0.01);
    }
}
