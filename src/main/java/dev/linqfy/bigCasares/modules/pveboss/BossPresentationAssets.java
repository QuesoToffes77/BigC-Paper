package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossPresentationAssets(
    String javaModelKey,
    String bedrockEntityIdentifier,
    String fallbackEntityType,
    String bossBarIconKey,
    String customSoundKey,
    String fallbackSoundKey
) {

    public BossPresentationAssets {
        Objects.requireNonNull(javaModelKey, "javaModelKey");
        Objects.requireNonNull(bedrockEntityIdentifier, "bedrockEntityIdentifier");
        if (fallbackEntityType == null || fallbackEntityType.isBlank()) {
            throw new IllegalArgumentException("fallbackEntityType must not be blank");
        }
        Objects.requireNonNull(bossBarIconKey, "bossBarIconKey");
        Objects.requireNonNull(customSoundKey, "customSoundKey");
        if (fallbackSoundKey == null || fallbackSoundKey.isBlank()) {
            throw new IllegalArgumentException("fallbackSoundKey must not be blank");
        }
    }
}
