package dev.linqfy.bigCasares.modules.environment;

import dev.linqfy.bigCasares.modules.acidrain.AcidRainLevel;
import dev.linqfy.bigCasares.modules.acidrain.AcidRainVisualProfiles;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentalVisualProfileTest {

    @Test
    void acidRainLevelsSelectThreeDistinctVisualIdentities() {
        EnvironmentalVisualProfile acid = AcidRainVisualProfiles.forLevel(AcidRainLevel.ACID);
        EnvironmentalVisualProfile toxic = AcidRainVisualProfiles.forLevel(AcidRainLevel.TOXIC);
        EnvironmentalVisualProfile chemical = AcidRainVisualProfiles.forLevel(AcidRainLevel.CHEMICAL);

        assertEquals(EnvironmentalVisualProfile.ACID_RAIN, acid);
        assertEquals(EnvironmentalVisualProfile.TOXIC_SPORES, toxic);
        assertEquals(EnvironmentalVisualProfile.CHEMICAL_FOG, chemical);
        assertNotEquals(acid, toxic);
        assertNotEquals(toxic, chemical);
    }

    @Test
    void allEnvironmentalEventsHaveUniqueProfiles() {
        assertEquals(4, EnumSet.allOf(EnvironmentalVisualProfile.class).size());
        assertTrue(EnumSet.allOf(EnvironmentalVisualProfile.class)
            .contains(EnvironmentalVisualProfile.BLOOD_MOON));
    }

    @Test
    void visualQualityScalesDensityWithinHardBudget() {
        EnvironmentalVisualPlan low = EnvironmentalVisualPlan.forProfile(
            EnvironmentalVisualProfile.CHEMICAL_FOG, EnvironmentalVisualQuality.LOW);
        EnvironmentalVisualPlan medium = EnvironmentalVisualPlan.forProfile(
            EnvironmentalVisualProfile.CHEMICAL_FOG, EnvironmentalVisualQuality.MEDIUM);
        EnvironmentalVisualPlan high = EnvironmentalVisualPlan.forProfile(
            EnvironmentalVisualProfile.CHEMICAL_FOG, EnvironmentalVisualQuality.HIGH);

        assertTrue(low.particleBudget() < medium.particleBudget());
        assertTrue(medium.particleBudget() < high.particleBudget());
        assertTrue(high.particleBudget() <= EnvironmentalVisualPlan.HARD_MAX_PARTICLES_PER_PLAYER_CYCLE);
    }
}
