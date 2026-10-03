package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GliderVisualModelTest {

    @Test
    void everyTierUsesItsOwnDeployedModel() {
        for (GliderTier tier : GliderTier.values()) {
            assertEquals(
                "bigcasares:glider_deployed_tier_" + tier.number(),
                GliderVisualController.deployedModelKey(tier).toString()
            );
        }
    }
}
