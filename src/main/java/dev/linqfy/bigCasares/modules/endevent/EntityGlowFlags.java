package dev.linqfy.bigCasares.modules.endevent;

final class EntityGlowFlags {
    private static final int GLOWING_BIT = 0x40;

    private EntityGlowFlags() {
    }

    static byte withGlow(byte sharedFlags, boolean glowing) {
        return (byte) (glowing
            ? sharedFlags | GLOWING_BIT
            : sharedFlags & ~GLOWING_BIT);
    }
}
