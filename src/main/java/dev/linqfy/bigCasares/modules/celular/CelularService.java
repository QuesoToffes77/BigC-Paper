package dev.linqfy.bigCasares.modules.celular;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Video navigation and village loot rules, free of Bukkit so they can be unit tested. */
public final class CelularService {

    private final List<CelularVideo> videos;
    private final CelularSettings settings;

    public CelularService(List<CelularVideo> videos, CelularSettings settings) {
        if (videos.isEmpty()) {
            throw new IllegalArgumentException("The phone needs at least one video");
        }
        this.videos = List.copyOf(videos);
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public CelularSettings settings() {
        return settings;
    }

    public List<CelularVideo> videos() {
        return videos;
    }

    public int size() {
        return videos.size();
    }

    public CelularVideo video(int index) {
        return videos.get(clamp(index));
    }

    /** F: next video, wrapping after the last one. */
    public int next(int index) {
        return Math.floorMod(clamp(index) + 1, videos.size());
    }

    /** Shift + F: previous video, wrapping before the first one. */
    public int previous(int index) {
        return Math.floorMod(clamp(index) - 1, videos.size());
    }

    public int navigate(int index, boolean sneaking) {
        return sneaking ? previous(index) : next(index);
    }

    /**
     * Swap hands: F on Java (Shift + F goes back). Bedrock has no swap key; Geyser's EmoteOffhand extension turns
     * the emote (D-pad left on a controller) into a swap, and there it goes back because D-pad down goes forward.
     */
    public int onSwapHands(int index, boolean sneaking, boolean bedrock) {
        return bedrock ? previous(index) : navigate(index, sneaking);
    }

    /** Drop is D-pad down on a Bedrock controller. Java keeps Q as a normal drop (its client predicts the drop). */
    public boolean dropChangesVideo(boolean bedrock, boolean sneaking) {
        return bedrock && !sneaking;
    }

    public String hint(boolean bedrock) {
        return bedrock ? "  ·  Abajo siguiente, emote anterior" : "  ·  F siguiente, Shift+F anterior";
    }

    /** An index saved in an old phone can fall out of range when videos are removed. */
    public int clamp(int index) {
        return index < 0 || index >= videos.size() ? 0 : index;
    }

    public String actionBar(int index) {
        return "▶ " + (clamp(index) + 1) + "/" + videos.size() + "  " + video(index).title();
    }

    public static boolean isVillageChest(String namespace, String path) {
        return "minecraft".equals(namespace) && path.toLowerCase(Locale.ROOT).startsWith("chests/village/");
    }

    /** Each village hands out a single phone: in the first chest somebody opens. */
    public boolean shouldAddVillagePhone(String lootNamespace, String lootPath, boolean villageAlreadyGavePhone) {
        return settings.villageLootEnabled()
            && isVillageChest(lootNamespace, lootPath)
            && !villageAlreadyGavePhone;
    }
}
