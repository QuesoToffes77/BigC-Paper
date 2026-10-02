package dev.linqfy.bigCasares.modules.bloodmoon;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class BloodMoonSpawnPolicy {
    private BloodMoonSpawnPolicy() {
    }

    static boolean distanceAllowed(double distance, double minimum, double maximum) {
        return Double.isFinite(distance) && distance >= minimum && distance <= maximum;
    }

    static boolean pressureAllows(BloodMoonSpawningSettings settings, BloodMoonSpawnPressure pressure) {
        if (settings == null || pressure == null || settings.multiplier() <= 1.0
            || settings.maxExtraHostilesPerPlayer() <= 0 || settings.maxExtraHostilesPerWorld() <= 0) {
            return false;
        }
        int vanillaCeiling = pressure.vanillaMonsterLimit() <= 0
            ? Integer.MAX_VALUE
            : (int) Math.ceil(pressure.vanillaMonsterLimit() * settings.multiplier());
        return pressure.playerExtra() < settings.maxExtraHostilesPerPlayer()
            && pressure.worldExtra() < settings.maxExtraHostilesPerWorld()
            && pressure.loadedHostiles() < vanillaCeiling;
    }

    static int extraAttemptsPerPlayer(double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier <= 1.0) {
            return 0;
        }
        return Math.min(8, (int) Math.ceil(multiplier - 1.0));
    }
}

record BloodMoonSpawnPressure(int playerExtra, int worldExtra, int loadedHostiles, int vanillaMonsterLimit) {
}

final class BloodMoonSpawnLedger {
    private final Map<String, String> ownerByMob = new LinkedHashMap<>();

    boolean tryReserve(String playerId, String mobId, int perPlayerLimit, int worldLimit) {
        if (playerId == null || playerId.isBlank() || mobId == null || mobId.isBlank()
            || perPlayerLimit <= 0 || worldLimit <= 0 || ownerByMob.containsKey(mobId)
            || worldCount() >= worldLimit || playerCount(playerId) >= perPlayerLimit) {
            return false;
        }
        ownerByMob.put(mobId, playerId);
        return true;
    }

    void release(String mobId) {
        ownerByMob.remove(mobId);
    }

    int playerCount(String playerId) {
        return (int) ownerByMob.values().stream().filter(playerId::equals).count();
    }

    int worldCount() {
        return ownerByMob.size();
    }

    Set<String> mobIds() {
        return Set.copyOf(ownerByMob.keySet());
    }

    void clear() {
        ownerByMob.clear();
    }
}
