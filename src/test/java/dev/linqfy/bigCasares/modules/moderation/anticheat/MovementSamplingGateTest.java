package dev.linqfy.bigCasares.modules.moderation.anticheat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementSamplingGateTest {
    @Test void samplesAtMostOncePerPlayerPerTick() {
        assertTrue(MovementCheck.shouldSampleTick(null, 42));
        assertFalse(MovementCheck.shouldSampleTick(42, 42));
        assertTrue(MovementCheck.shouldSampleTick(41, 42));
    }
}
