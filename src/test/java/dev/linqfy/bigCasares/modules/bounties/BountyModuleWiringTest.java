package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BountyModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("bounty-system", new BountyModule(null).getId());
    }

    @Test
    void rejectsInvalidMinimumBounty() {
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new BountySettings(0.30, 0.20, 100.0, -1.0)
        );

        assertEquals("minimum-bounty no puede ser negativo.", thrown.getMessage());
    }
}
