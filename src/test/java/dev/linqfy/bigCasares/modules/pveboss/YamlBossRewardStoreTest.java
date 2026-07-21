package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlBossRewardStoreTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void pendingRewardsSurviveStoreRecreationAndKeepTheirUniqueIds() {
        Path file = temporaryDirectory.resolve("boss-rewards.yml");
        UUID playerId = UUID.randomUUID();
        PendingBossReward expected = new PendingBossReward(
            UUID.randomUUID(), playerId, new BossReward("sahurs_bat", 1));

        new YamlBossRewardStore(file).save(expected);

        assertEquals(java.util.List.of(expected), new YamlBossRewardStore(file).pending(playerId));
    }

    @Test
    void removingDeliveredRewardIsDurableAndDoesNotRemoveAnotherPlayersReward() {
        Path file = temporaryDirectory.resolve("boss-rewards.yml");
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        PendingBossReward first = new PendingBossReward(
            UUID.randomUUID(), firstPlayer, new BossReward("ECHO_SHARD", 3));
        PendingBossReward second = new PendingBossReward(
            UUID.randomUUID(), secondPlayer, new BossReward("ENCHANTED_GOLDEN_APPLE", 3));
        YamlBossRewardStore store = new YamlBossRewardStore(file);
        store.save(first);
        store.save(second);

        store.remove(first.rewardId());

        YamlBossRewardStore reloaded = new YamlBossRewardStore(file);
        assertTrue(reloaded.pending(firstPlayer).isEmpty());
        assertEquals(java.util.List.of(second), reloaded.pending(secondPlayer));
    }
}
