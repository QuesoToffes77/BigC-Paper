package dev.linqfy.bigCasares.modules.glider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderGripPoseControllerTest {

    @Test
    void usesATwoHandedPoseThatCannotConsumeDuringAMaximumSession() {
        assertEquals("CROSSBOW", GliderGripPosePolicy.animationName());
        assertTrue(GliderGripPosePolicy.consumeSeconds() > 12_000.0F / 20.0F);
    }
}
