package dev.linqfy.bigCasares.modules.nexus;

import java.util.Locale;

@FunctionalInterface
public interface NexusPlacementProbe {

    String materialAt(NexusBlockPosition position);

    default boolean isPassable(NexusBlockPosition position) {
        String material = materialAt(position);
        if (material == null) {
            return false;
        }
        String normalized = material.toUpperCase(Locale.ROOT);
        int namespaceSeparator = normalized.indexOf(':');
        if (namespaceSeparator >= 0) {
            normalized = normalized.substring(namespaceSeparator + 1);
        }
        return normalized.equals("AIR")
                || normalized.equals("CAVE_AIR")
                || normalized.equals("VOID_AIR");
    }
}
