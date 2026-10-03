package dev.linqfy.bigCasares.modules.copperapple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperAppleBalanceTest {

    @Test
    void keepsCopperAppleFoodProfile() {
        assertEquals(4, CopperAppleBalance.NUTRITION);
        assertEquals(2.4f, CopperAppleBalance.SATURATION);
    }

    @Test
    void oxidationStrengthensEffectsAndLightning() {
        CopperAppleEffectProfile fresh = CopperAppleBalance.profileFor(CopperAppleOxidationStage.FRESH);
        CopperAppleEffectProfile exposed = CopperAppleBalance.profileFor(CopperAppleOxidationStage.EXPOSED);
        CopperAppleEffectProfile weathered = CopperAppleBalance.profileFor(CopperAppleOxidationStage.WEATHERED);
        CopperAppleEffectProfile oxidized = CopperAppleBalance.profileFor(CopperAppleOxidationStage.OXIDIZED);

        assertEquals(0, fresh.speedTicks());
        assertTrue(exposed.speedTicks() > 0, "Exposed apples grant Speed I");
        assertEquals(0, exposed.strengthTicks());
        assertTrue(weathered.strengthTicks() > 0, "Weathered apples grant Strength I");
        assertTrue(oxidized.strengthTicks() > weathered.strengthTicks());
        assertTrue(fresh.absorptionTicks() < exposed.absorptionTicks());
        assertTrue(exposed.absorptionTicks() <= weathered.absorptionTicks());
        assertTrue(weathered.absorptionTicks() < oxidized.absorptionTicks());
        assertEquals(0, fresh.lightningChancePercent());
        assertTrue(exposed.lightningChancePercent() < weathered.lightningChancePercent());
        assertEquals(100, oxidized.lightningChancePercent());
    }

    @Test
    void strengthAndSpeedRemainLevelOne() {
        for (CopperAppleOxidationStage stage : CopperAppleOxidationStage.values()) {
            CopperAppleEffectProfile profile = CopperAppleBalance.profileFor(stage);
            assertEquals(0, profile.speedAmplifier());
            assertEquals(0, profile.strengthAmplifier());
        }
    }

    @Test
    void oxidizedLoreDescribesActualEffectsWithoutChangingWithTime() {
        String lore = String.join(" ", CopperAppleOxidationStage.OXIDIZED.lore());
        assertTrue(lore.contains("Velocidad I"));
        assertTrue(lore.contains("(90s)"));
        assertTrue(lore.contains("Fuerza I"));
        assertTrue(lore.contains("(60s)"));
        assertTrue(lore.contains("sin dano"));
        assertEquals(CopperAppleOxidationStage.OXIDIZED.lore(), CopperAppleOxidationStage.OXIDIZED.lore());
    }
}
