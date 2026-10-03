package dev.linqfy.bigCasares.modules.acidrain;

import java.util.LinkedHashSet;
import java.util.Set;

final class AcidRainPresentationRegistry {
    private final Set<String> bossBars = new LinkedHashSet<>();

    void ensureBossBar(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            bossBars.add(playerId);
        }
    }

    void removeBossBar(String playerId) {
        bossBars.remove(playerId);
    }

    int bossBarCount() {
        return bossBars.size();
    }

    void reload() {
        // Reload keeps existing per-player handles and updates them in place in the Bukkit runtime.
    }

    void clear() {
        bossBars.clear();
    }
}
