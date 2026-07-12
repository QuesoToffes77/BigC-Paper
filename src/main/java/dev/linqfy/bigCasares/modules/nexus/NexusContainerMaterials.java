package dev.linqfy.bigCasares.modules.nexus;

import org.bukkit.Material;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class NexusContainerMaterials {

    private static final Set<String> CONTAINERS = Set.of(
            "CHEST",
            "TRAPPED_CHEST",
            "BARREL",
            "SHULKER_BOX",
            "ENDER_CHEST",
            "HOPPER",
            "FURNACE",
            "BLAST_FURNACE",
            "SMOKER",
            "DISPENSER",
            "DROPPER",
            "CHISELED_BOOKSHELF",
            "DECORATED_POT",
            "CRAFTER",
            "BREWING_STAND"
    );

    private NexusContainerMaterials() {
    }

    public static boolean isContainer(Material material) {
        return material != null && isContainer(material.name());
    }

    public static boolean isContainer(String materialId) {
        if (materialId == null || materialId.isBlank()) {
            return false;
        }
        String normalized = materialId.strip().toUpperCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return CONTAINERS.contains(normalized) || normalized.endsWith("_SHULKER_BOX");
    }

    public static Set<String> names() {
        return CONTAINERS;
    }
}
