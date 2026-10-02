package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualProfile;

public final class AcidRainVisualProfiles {
    private AcidRainVisualProfiles() {
    }

    public static EnvironmentalVisualProfile forLevel(AcidRainLevel level) {
        return switch (level == null ? AcidRainLevel.ACID : level) {
            case ACID -> EnvironmentalVisualProfile.ACID_RAIN;
            case TOXIC -> EnvironmentalVisualProfile.TOXIC_SPORES;
            case CHEMICAL -> EnvironmentalVisualProfile.CHEMICAL_FOG;
        };
    }
}
