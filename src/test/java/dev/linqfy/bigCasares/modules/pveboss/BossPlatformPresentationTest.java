package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPlatformPresentationTest {

    private final BossPlatformPresentationResolver resolver = new BossPlatformPresentationResolver();
    private final BossPresentationAssets assets = new BossPresentationAssets(
        "bigcasares:abyss_guardian",
        "bigcasares:abyss_guardian",
        "minecraft:warden",
        "bigcasares:boss.abyss_icon",
        "bigcasares:boss.abyss_theme",
        "minecraft:music_disc.5"
    );

    @Test
    void resolvesJavaAndBedrockResourcePackPresentations() {
        BossPlatformPresentation java = resolver.resolve(
            BossClientPlatform.JAVA,
            BossResourcePackStatus.LOADED,
            assets
        );
        BossPlatformPresentation bedrock = resolver.resolve(
            BossClientPlatform.BEDROCK,
            BossResourcePackStatus.LOADED,
            assets
        );

        assertEquals(BossPresentationMode.JAVA_RESOURCE_PACK, java.mode());
        assertEquals(BossPresentationMode.BEDROCK_RESOURCE_PACK, bedrock.mode());
        assertTrue(java.customAssets());
        assertTrue(bedrock.customAssets());
    }

    @Test
    void fallsBackToVanillaWhenTheResourcePackWasNotLoaded() {
        BossPlatformPresentation presentation = resolver.resolve(
            BossClientPlatform.JAVA,
            BossResourcePackStatus.DECLINED,
            assets
        );

        assertEquals(BossPresentationMode.VANILLA_FALLBACK, presentation.mode());
        assertEquals("minecraft:warden", presentation.entityPresentation());
        assertEquals("minecraft:music_disc.5", presentation.soundKey());
        assertFalse(presentation.customAssets());
    }
}
