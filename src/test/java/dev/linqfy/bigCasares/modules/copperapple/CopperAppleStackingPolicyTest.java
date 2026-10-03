package dev.linqfy.bigCasares.modules.copperapple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopperAppleStackingPolicyTest {

    @Test
    void independentlyCreatedApplesInTheSameScanWindowShareCanonicalAge() {
        assertEquals(1_000_000L, CopperAppleStackingPolicy.canonicalCreatedAt(1_000_001L, 5_000L));
        assertEquals(1_000_000L, CopperAppleStackingPolicy.canonicalCreatedAt(1_004_999L, 5_000L));
    }

    @Test
    void mergingStacksAlwaysKeepsTheOldestValidAge() {
        assertEquals(900_000L, CopperAppleStackingPolicy.oldestCreatedAt(900_000L, 1_000_000L));
        assertEquals(900_000L, CopperAppleStackingPolicy.oldestCreatedAt(1_000_000L, 900_000L));
    }

    @Test
    void invalidAgeFallsBackToTheOtherStackInsteadOfResettingBoth() {
        assertEquals(900_000L, CopperAppleStackingPolicy.oldestCreatedAt(-1L, 900_000L));
        assertEquals(900_000L, CopperAppleStackingPolicy.oldestCreatedAt(900_000L, -1L));
    }
}
