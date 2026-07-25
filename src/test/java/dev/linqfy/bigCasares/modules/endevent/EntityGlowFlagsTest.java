package dev.linqfy.bigCasares.modules.endevent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityGlowFlagsTest {

    @Test
    void enablesGlowWithoutChangingOtherSharedFlags() {
        byte sneakingAndInvisible = 0x22;

        assertEquals((byte) 0x62, EntityGlowFlags.withGlow(sneakingAndInvisible, true));
    }

    @Test
    void disablesGlowWithoutChangingOtherSharedFlags() {
        byte sneakingInvisibleAndGlowing = 0x62;

        assertEquals((byte) 0x22, EntityGlowFlags.withGlow(sneakingInvisibleAndGlowing, false));
    }
}
