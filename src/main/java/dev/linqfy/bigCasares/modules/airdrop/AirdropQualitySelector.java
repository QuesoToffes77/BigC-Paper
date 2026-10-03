package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class AirdropQualitySelector {
    private final Map<AirdropQuality, Double> chances;

    public AirdropQualitySelector(Map<AirdropQuality, Double> chances) {
        this.chances = Map.copyOf(Objects.requireNonNull(chances, "chances"));
        double total = this.chances.values().stream().mapToDouble(Double::doubleValue).sum();
        if (this.chances.size() != AirdropQuality.values().length || Math.abs(total - 1.0) > 0.000_001) {
            throw new IllegalArgumentException("quality chances must define all qualities and sum to 1.0");
        }
    }

    public AirdropQuality roll(Random random) {
        double roll = Objects.requireNonNull(random, "random").nextDouble();
        double cumulative = 0.0;
        for (AirdropQuality quality : AirdropQuality.values()) {
            cumulative += chances.get(quality);
            if (roll < cumulative) {
                return quality;
            }
        }
        return AirdropQuality.GHISTIC;
    }
}
