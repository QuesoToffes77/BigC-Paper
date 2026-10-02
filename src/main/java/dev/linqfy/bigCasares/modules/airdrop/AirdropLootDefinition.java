package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Locale;
import java.util.Objects;

public record AirdropLootDefinition(
    AirdropRewardKind kind,
    String itemId,
    int minimumAmount,
    int maximumAmount,
    int weight,
    boolean unique
) {
    public AirdropLootDefinition {
        kind = Objects.requireNonNull(kind, "kind");
        itemId = Objects.requireNonNull(itemId, "itemId").toLowerCase(Locale.ROOT);
        if (minimumAmount < 1 || maximumAmount < minimumAmount || weight < 1) {
            throw new IllegalArgumentException("invalid airdrop loot definition");
        }
    }

    public static AirdropLootDefinition material(String id, int min, int max, int weight) {
        return new AirdropLootDefinition(AirdropRewardKind.MATERIAL, id, min, max, weight, false);
    }

    public static AirdropLootDefinition custom(String id, int weight) {
        return new AirdropLootDefinition(AirdropRewardKind.CUSTOM_ITEM, id, 1, 1, weight, true);
    }

    public AirdropReward roll(AirdropQuality quality, RandomSource random) {
        int amount = minimumAmount + random.nextInt(maximumAmount - minimumAmount + 1);
        return kind == AirdropRewardKind.CUSTOM_ITEM
            ? AirdropReward.customItem(itemId, amount, quality)
            : AirdropReward.material(itemId, amount, quality);
    }

    @FunctionalInterface
    public interface RandomSource {
        int nextInt(int bound);
    }
}
