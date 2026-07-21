package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossReward(String itemId, int amount) {

    public BossReward {
        itemId = Objects.requireNonNull(itemId, "itemId").strip();
        if (itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId cannot be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    public static BossReward forSahurPlacement(int placement) {
        return switch (placement) {
            case 1 -> new BossReward("sahurs_bat", 1);
            case 2 -> new BossReward("ENCHANTED_GOLDEN_APPLE", 3);
            case 3 -> new BossReward("ECHO_SHARD", 3);
            default -> throw new IllegalArgumentException("Sahur placement must be between 1 and 3");
        };
    }
}
