package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class AirdropQualityPromoter {
    private final Map<AirdropQuality, Double> chances;

    public AirdropQualityPromoter(Map<AirdropQuality, Double> chances) {
        this.chances = Map.copyOf(Objects.requireNonNull(chances, "chances"));
    }

    public AirdropQuality promote(AirdropQuality base, Random random) {
        AirdropQuality current = Objects.requireNonNull(base, "base");
        Objects.requireNonNull(random, "random");
        while (current != AirdropQuality.GHISTIC) {
            double chance = chances.getOrDefault(current, 0.0);
            if (chance <= 0.0 || random.nextDouble() >= chance) {
                break;
            }
            current = current.next().orElse(AirdropQuality.GHISTIC);
        }
        return current;
    }
}
