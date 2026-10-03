package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.modules.model.JavaModelKeys;

/** Stable visual model selection for each Acid Rain mob family. */
final class AcidRainMobModels {

    private AcidRainMobModels() {
    }

    static String modelKeyFor(AcidRainMobType type) {
        return switch (type) {
            case CRAWLER -> JavaModelKeys.TOXIC_CRAWLER;
            case BRUTE -> JavaModelKeys.TOXIC_BRUTE;
            case SPITTER -> JavaModelKeys.TOXIC_SPITTER;
        };
    }
}
