package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.Material;

import java.util.Set;

public final class EndEventPotionPolicy {
    private static final Set<Material> FORBIDDEN = Set.of(
        Material.POTION,
        Material.SPLASH_POTION,
        Material.LINGERING_POTION,
        Material.TIPPED_ARROW
    );

    private EndEventPotionPolicy() {
    }

    public static boolean isForbidden(Material material) {
        return FORBIDDEN.contains(material);
    }
}
