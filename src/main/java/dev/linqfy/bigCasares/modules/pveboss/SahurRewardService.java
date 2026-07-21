package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SahurRewardService {

    public interface DeliveryGateway {
        boolean isOnline(UUID playerId);

        void giveOrDrop(UUID playerId, BossReward reward);
    }

    private final BossRewardStore store;
    private final DeliveryGateway delivery;

    public SahurRewardService(BossRewardStore store, DeliveryGateway delivery) {
        this.store = Objects.requireNonNull(store, "store");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    public synchronized UUID award(UUID playerId, BossReward reward) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(reward, "reward");
        PendingBossReward pending = new PendingBossReward(UUID.randomUUID(), playerId, reward);
        store.save(pending);
        if (delivery.isOnline(playerId)) {
            deliverPending(playerId);
        }
        return pending.rewardId();
    }

    public synchronized int deliverPending(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (!delivery.isOnline(playerId)) {
            return 0;
        }
        int delivered = 0;
        List<PendingBossReward> pending = store.pending(playerId);
        for (PendingBossReward reward : pending) {
            try {
                delivery.giveOrDrop(playerId, reward.reward());
            } catch (RuntimeException failure) {
                break;
            }
            store.remove(reward.rewardId());
            delivered++;
        }
        return delivered;
    }

    public int deliverPendingForOnline(Collection<UUID> playerIds) {
        Objects.requireNonNull(playerIds, "playerIds");
        int delivered = 0;
        for (UUID playerId : List.copyOf(playerIds)) {
            delivered += deliverPending(playerId);
        }
        return delivered;
    }
}
