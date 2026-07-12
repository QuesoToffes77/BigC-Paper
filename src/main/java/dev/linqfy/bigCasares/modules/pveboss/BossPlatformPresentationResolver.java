package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public final class BossPlatformPresentationResolver {

    public BossPlatformPresentation resolve(
        BossClientPlatform platform,
        BossResourcePackStatus resourcePackStatus,
        BossPresentationAssets assets
    ) {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(resourcePackStatus, "resourcePackStatus");
        Objects.requireNonNull(assets, "assets");

        if (resourcePackStatus == BossResourcePackStatus.LOADED) {
            if (platform == BossClientPlatform.JAVA && !assets.javaModelKey().isBlank()) {
                return custom(
                    BossPresentationMode.JAVA_RESOURCE_PACK,
                    assets.javaModelKey(),
                    assets
                );
            }
            if (platform == BossClientPlatform.BEDROCK && !assets.bedrockEntityIdentifier().isBlank()) {
                return custom(
                    BossPresentationMode.BEDROCK_RESOURCE_PACK,
                    assets.bedrockEntityIdentifier(),
                    assets
                );
            }
        }

        return new BossPlatformPresentation(
            BossPresentationMode.VANILLA_FALLBACK,
            assets.fallbackEntityType(),
            "",
            assets.fallbackSoundKey(),
            false
        );
    }

    public BossPlatformPresentation resolve(
        BossClientPlatform platform,
        boolean resourcePackLoaded,
        BossPresentationAssets assets
    ) {
        return resolve(
            platform,
            resourcePackLoaded ? BossResourcePackStatus.LOADED : BossResourcePackStatus.NOT_LOADED,
            assets
        );
    }

    private static BossPlatformPresentation custom(
        BossPresentationMode mode,
        String entityPresentation,
        BossPresentationAssets assets
    ) {
        String sound = assets.customSoundKey().isBlank()
            ? assets.fallbackSoundKey()
            : assets.customSoundKey();
        return new BossPlatformPresentation(
            mode,
            entityPresentation,
            assets.bossBarIconKey(),
            sound,
            true
        );
    }
}
