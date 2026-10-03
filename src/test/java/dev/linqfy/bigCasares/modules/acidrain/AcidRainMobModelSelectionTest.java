package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.modules.model.JavaModelKeys;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcidRainMobModelSelectionTest {

    @Test
    void everyAcidMobFamilyUsesADifferentModel() {
        assertEquals(JavaModelKeys.TOXIC_CRAWLER,
            AcidRainMobModels.modelKeyFor(AcidRainMobType.CRAWLER));
        assertEquals(JavaModelKeys.TOXIC_BRUTE,
            AcidRainMobModels.modelKeyFor(AcidRainMobType.BRUTE));
        assertEquals(JavaModelKeys.TOXIC_SPITTER,
            AcidRainMobModels.modelKeyFor(AcidRainMobType.SPITTER));

        assertEquals(3, Set.of(
            AcidRainMobModels.modelKeyFor(AcidRainMobType.CRAWLER),
            AcidRainMobModels.modelKeyFor(AcidRainMobType.BRUTE),
            AcidRainMobModels.modelKeyFor(AcidRainMobType.SPITTER)
        ).size());
    }
}
