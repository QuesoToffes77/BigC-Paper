package dev.linqfy.bigCasares.modules.copperapple;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CopperAppleOxidationPolicyTest {

    private static final long CREATED_AT = 1_000_000L;
    private final CopperAppleOxidationPolicy policy = CopperAppleOxidationPolicy.fromSeconds(300, 900, 1800);

    @Test
    void advancesThroughEveryStageAtConfiguredTimes() {
        assertEquals(CopperAppleOxidationStage.FRESH, policy.stageAt(CREATED_AT, CREATED_AT));
        assertEquals(CopperAppleOxidationStage.FRESH, policy.stageAt(CREATED_AT, CREATED_AT + 299_999L));
        assertEquals(CopperAppleOxidationStage.EXPOSED, policy.stageAt(CREATED_AT, CREATED_AT + 300_000L));
        assertEquals(CopperAppleOxidationStage.WEATHERED, policy.stageAt(CREATED_AT, CREATED_AT + 900_000L));
        assertEquals(CopperAppleOxidationStage.OXIDIZED, policy.stageAt(CREATED_AT, CREATED_AT + 1_800_000L));
    }

    @Test
    void clockMovingBackwardsCannotOxidizeAnApple() {
        assertEquals(CopperAppleOxidationStage.FRESH, policy.stageAt(CREATED_AT, CREATED_AT - 1L));
    }

    @Test
    void reportsTimeUntilTheNextStageWithoutChangingItemMetadata() {
        assertEquals(300_000L, policy.millisUntilNextStage(CREATED_AT, CREATED_AT));
        assertEquals(1L, policy.millisUntilNextStage(CREATED_AT, CREATED_AT + 299_999L));
        assertEquals(600_000L, policy.millisUntilNextStage(CREATED_AT, CREATED_AT + 300_000L));
        assertEquals(900_000L, policy.millisUntilNextStage(CREATED_AT, CREATED_AT + 900_000L));
        assertEquals(0L, policy.millisUntilNextStage(CREATED_AT, CREATED_AT + 1_800_000L));
    }

    @Test
    void rejectsNegativeOrNonMonotonicThresholds() {
        assertThrows(IllegalArgumentException.class, () -> new CopperAppleOxidationPolicy(-1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new CopperAppleOxidationPolicy(10, 9, 11));
        assertThrows(IllegalArgumentException.class, () -> new CopperAppleOxidationPolicy(10, 20, 19));
    }
}
