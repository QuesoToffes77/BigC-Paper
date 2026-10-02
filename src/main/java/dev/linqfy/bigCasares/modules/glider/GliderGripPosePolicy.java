package dev.linqfy.bigCasares.modules.glider;

/** Stable values for the two-handed vanilla pose used while gliding. */
final class GliderGripPosePolicy {

    private static final String ANIMATION_NAME = "CROSSBOW";
    private static final float CONSUME_SECONDS = 3_600.0F;

    private GliderGripPosePolicy() {
    }

    static String animationName() {
        return ANIMATION_NAME;
    }

    static float consumeSeconds() {
        return CONSUME_SECONDS;
    }
}
