package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public final class SahurRewardService {

    public interface DeliveryGateway {
        boolean isOnline(UUID playerId);

        void giveOrDrop(UUID playerId, BossReward reward);
    }

    private final BossRewardStore store;
    private final DeliveryGateway delivery;
    private final Predicate<UUID> ready;
    private final Set<UUID> deferred = new LinkedHashSet<>();

    public SahurRewardService(BossRewardStore store, DeliveryGateway delivery) {
        this(store, delivery, ignored -> true);
    }

    public SahurRewardService(BossRewardStore store, DeliveryGateway delivery, Predicate<UUID> ready) {
        this.store = Objects.requireNonNull(store, "store");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
        this.ready = Objects.requireNonNull(ready, "ready");
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
            deferred.remove(playerId);
            return 0;
        }
        if (!ready.test(playerId)) {
            deferred.add(playerId);
            return 0;
        }
        deferred.remove(playerId);
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

    /** Retry only players deferred by authentication or another inventory owner. */
    public synchronized void retryDeferred() {
        for (UUID playerId : List.copyOf(deferred)) deliverPending(playerId);
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
