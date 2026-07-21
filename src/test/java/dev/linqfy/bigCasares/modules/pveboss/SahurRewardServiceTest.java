package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SahurRewardServiceTest {

    @Test
    void prizesMatchDamagePlacementsExactly() {
        assertEquals(new BossReward("sahurs_bat", 1), BossReward.forSahurPlacement(1));
        assertEquals(new BossReward("ENCHANTED_GOLDEN_APPLE", 3), BossReward.forSahurPlacement(2));
        assertEquals(new BossReward("ECHO_SHARD", 3), BossReward.forSahurPlacement(3));
        assertThrows(IllegalArgumentException.class, () -> BossReward.forSahurPlacement(4));
    }

    @Test
    void disconnectedWinnerReceivesPendingRewardOnlyOnce() {
        UUID winner = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        InMemoryStore store = new InMemoryStore(events);
        RecordingDelivery delivery = new RecordingDelivery(events);
        SahurRewardService service = new SahurRewardService(store, delivery);

        service.award(winner, new BossReward("sahurs_bat", 1));
        assertEquals(1, store.pending(winner).size());

        delivery.online = true;
        service.deliverPending(winner);
        service.deliverPending(winner);

        assertEquals(1, delivery.delivered.size());
        assertTrue(store.pending(winner).isEmpty());
    }

    @Test
    void persistsBeforeAttemptingOnlineDeliveryAndRetainsRewardWhenDeliveryFails() {
        UUID winner = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        InMemoryStore store = new InMemoryStore(events);
        RecordingDelivery delivery = new RecordingDelivery(events);
        delivery.online = true;
        delivery.failure = new IllegalStateException("inventory unavailable");
        SahurRewardService service = new SahurRewardService(store, delivery);

        service.award(winner, new BossReward("ECHO_SHARD", 3));

        assertEquals(List.of("save", "deliver"), events);
        assertEquals(1, store.pending(winner).size());

        delivery.failure = null;
        service.deliverPending(winner);
        assertTrue(store.pending(winner).isEmpty());
        assertEquals(List.of(new BossReward("ECHO_SHARD", 3)), delivery.delivered);
    }

    @Test
    void successfulGiveOrDropIsRemovedOnlyAfterTheDeliveryGatewayReturns() {
        UUID winner = UUID.randomUUID();
        List<String> events = new ArrayList<>();
        InMemoryStore store = new InMemoryStore(events);
        RecordingDelivery delivery = new RecordingDelivery(events);
        delivery.online = true;
        SahurRewardService service = new SahurRewardService(store, delivery);

        service.award(winner, new BossReward("ENCHANTED_GOLDEN_APPLE", 3));

        assertEquals(List.of("save", "deliver", "remove"), events);
        assertTrue(store.pending(winner).isEmpty());
    }

    @Test
    void reEnableDeliveryAttemptsEveryCurrentlyOnlinePlayer() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        InMemoryStore store = new InMemoryStore();
        RecordingDelivery delivery = new RecordingDelivery();
        SahurRewardService service = new SahurRewardService(store, delivery);
        service.award(first, new BossReward("sahurs_bat", 1));
        service.award(second, new BossReward("ECHO_SHARD", 3));
        delivery.online = true;

        assertEquals(2, service.deliverPendingForOnline(List.of(first, second)));
        assertTrue(store.pending(first).isEmpty());
        assertTrue(store.pending(second).isEmpty());
    }

    private static final class InMemoryStore implements BossRewardStore {
        private final Map<UUID, PendingBossReward> rewards = new LinkedHashMap<>();
        private final List<String> events;

        private InMemoryStore() {
            this(new ArrayList<>());
        }

        private InMemoryStore(List<String> events) {
            this.events = events;
        }

        @Override
        public void save(PendingBossReward reward) {
            events.add("save");
            rewards.put(reward.rewardId(), reward);
        }

        @Override
        public List<PendingBossReward> pending(UUID playerId) {
            return rewards.values().stream().filter(value -> value.playerId().equals(playerId)).toList();
        }

        @Override
        public void remove(UUID rewardId) {
            events.add("remove");
            rewards.remove(rewardId);
        }
    }

    private static final class RecordingDelivery implements SahurRewardService.DeliveryGateway {
        private boolean online;
        private RuntimeException failure;
        private final List<String> events;
        private final List<BossReward> delivered = new ArrayList<>();

        private RecordingDelivery() {
            this(new ArrayList<>());
        }

        private RecordingDelivery(List<String> events) {
            this.events = events;
        }

        @Override
        public boolean isOnline(UUID playerId) {
            return online;
        }

        @Override
        public void giveOrDrop(UUID playerId, BossReward reward) {
            events.add("deliver");
            if (failure != null) throw failure;
            delivered.add(reward);
        }
    }
}
