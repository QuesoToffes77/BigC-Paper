package dev.linqfy.bigCasares.modules.pveboss;

import java.util.List;
import java.util.UUID;

public interface BossRewardStore {
    void save(PendingBossReward reward);

    List<PendingBossReward> pending(UUID playerId);

    void remove(UUID rewardId);
}
