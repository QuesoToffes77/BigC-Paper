package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropCustomItemValidatorTest {

    @Test
    void everyConfiguredRareItemIsValidatedAgainstTheCanonicalRegistry() {
        assertTrue(AirdropCustomItemValidator.configuredIds().contains("grappling_hook_6"));
        assertTrue(AirdropCustomItemValidator.configuredIds().contains("glider_tier_6"));
        assertDoesNotThrow(() -> AirdropCustomItemValidator.validate(id -> true));
    }

    @Test
    void missingCustomItemFactoryFailsBeforeAnAirdropCanSpawn() {
        assertThrows(IllegalStateException.class,
            () -> AirdropCustomItemValidator.validate(id -> !id.equals("glider_tier_6")));
    }
}
