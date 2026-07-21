package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.UUID;

public record PendingBossReward(UUID rewardId, UUID playerId, BossReward reward) {
    public PendingBossReward {
        Objects.requireNonNull(rewardId, "rewardId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(reward, "reward");
    }
}
