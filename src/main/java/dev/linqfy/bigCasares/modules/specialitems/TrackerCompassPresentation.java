package dev.linqfy.bigCasares.modules.specialitems;

import java.util.List;

record TrackerCompassPresentation(String displayName, List<String> lore, boolean glint) {

    TrackerCompassPresentation {
        lore = List.copyOf(lore);
    }

    static TrackerCompassPresentation tracking(String targetName, long remainingMillis) {
        long seconds = roundedSeconds(remainingMillis);
        return new TrackerCompassPresentation(
            "§bBrújula Rastreadora §7- §f" + targetName,
            List.of("§7Objetivo: §f" + targetName, "§7Rastreo restante: §b" + seconds + "s"),
            true
        );
    }

    static TrackerCompassPresentation cooldown(long remainingMillis) {
        long seconds = roundedSeconds(remainingMillis);
        return new TrackerCompassPresentation(
            "§7Brújula Rastreadora",
            List.of("§7Recarga restante: §c" + seconds + "s"),
            false
        );
    }

    private static long roundedSeconds(long remainingMillis) {
        return Math.max(0L, (remainingMillis + 999L) / 1_000L);
    }
}
