package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CustomCrossbowLoadService {

    private final Map<UUID, ActiveLoad> activeLoads = new HashMap<>();

    public boolean start(
        UUID playerId,
        CustomCrossbowChargeType chargeType,
        int quickChargeLevel,
        Material offhandMaterial,
        int offhandAmount
    ) {
        return start(playerId, chargeType, quickChargeLevel, offhandMaterial, offhandAmount, 0, 1);
    }

    public boolean start(
        UUID playerId,
        CustomCrossbowChargeType chargeType,
        int quickChargeLevel,
        Material offhandMaterial,
        int offhandAmount,
        int fireworkPower,
        int chargeCount
    ) {
        if (activeLoads.containsKey(playerId) || offhandAmount <= 0) {
            return false;
        }

        activeLoads.put(playerId, new ActiveLoad(
            chargeType,
            offhandMaterial,
            offhandAmount,
            Math.max(0, fireworkPower),
            Math.max(1, chargeCount),
            CustomCrossbowRules.loadTicks(chargeType, quickChargeLevel)
        ));
        return true;
    }

    public Optional<CustomCrossbowLoadResult> tick(UUID playerId, Material currentOffhandMaterial, int currentOffhandAmount, int elapsedTicks) {
        ActiveLoad load = activeLoads.get(playerId);
        if (load == null) {
            return Optional.empty();
        }
        if (currentOffhandMaterial != load.offhandMaterial() || currentOffhandAmount < load.offhandAmount()) {
            activeLoads.remove(playerId);
            return Optional.empty();
        }
        if (elapsedTicks < load.requiredTicks()) {
            return Optional.empty();
        }

        activeLoads.remove(playerId);
        return Optional.of(new CustomCrossbowLoadResult(
            load.chargeType(),
            load.fireworkPower(),
            load.chargeCount(),
            CustomCrossbowRules.remainingOffhandAmountAfterCharge(currentOffhandAmount)
        ));
    }

    public boolean isLoading(UUID playerId) {
        return activeLoads.containsKey(playerId);
    }

    public void cancel(UUID playerId) {
        activeLoads.remove(playerId);
    }

    private record ActiveLoad(
        CustomCrossbowChargeType chargeType,
        Material offhandMaterial,
        int offhandAmount,
        int fireworkPower,
        int chargeCount,
        int requiredTicks
    ) {
    }
}
