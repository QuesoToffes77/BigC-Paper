package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BossBehaviourCategoryWeight {
    private final Map<BossBehaviourCategory, Double> currentWeights = new ConcurrentHashMap<>();
    private final Map<BossBehaviourCategory, Double> reductionAmounts = new ConcurrentHashMap<>();
    private final double recoveryPerTick;

    public BossBehaviourCategoryWeight(Map<BossBehaviourCategory, Double> defaultWeights, double recoveryPerTick) {
        this.recoveryPerTick = recoveryPerTick;
        for (BossBehaviourCategory category : BossBehaviourCategory.values()) {
            currentWeights.put(category, 1.0);
            reductionAmounts.put(category, defaultWeights.getOrDefault(category, 0.5));
        }
    }

    public double getWeight(BossBehaviourCategory category) {
        return currentWeights.getOrDefault(category, 1.0);
    }

    public void reduceWeight(BossBehaviourCategory category) {
        double current = getWeight(category);
        double reduction = reductionAmounts.getOrDefault(category, 0.5);
        currentWeights.put(category, Math.max(0.1, current * reduction));
    }

    public void normalizeWeights() {
        for (BossBehaviourCategory category : BossBehaviourCategory.values()) {
            double current = getWeight(category);
            if (current < 1.0) {
                currentWeights.put(category, Math.min(1.0, current + recoveryPerTick));
            }
        }
    }
}
