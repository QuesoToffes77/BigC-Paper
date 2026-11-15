package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.thirdparty;

import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicTemplateSource;

import java.util.List;

/** The third-party template sources credited in {@link ThirdPartyCredits}. */
public final class ThirdPartySources {

    private ThirdPartySources() {
    }

    /** YUNG's Better Dungeons rooms and Doomlike Dungeons layouts, in a fixed order. */
    public static List<MythicTemplateSource> all() {
        return List.of(new YungsBetterDungeonsSource(), new DoomlikeDungeonsSource());
    }
}
