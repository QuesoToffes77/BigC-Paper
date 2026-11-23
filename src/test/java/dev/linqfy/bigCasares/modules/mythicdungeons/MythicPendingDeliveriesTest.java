package dev.linqfy.bigCasares.modules.mythicdungeons;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MythicPendingDeliveriesTest {

    private static final Logger LOGGER = Logger.getLogger("mythic-pending-test");

    @TempDir
    Path folder;

    @Test
    void survivesARestartAndIsDeliveredOnlyOnce() {
        Path file = folder.resolve("data").resolve(MythicPendingDeliveries.FILE);
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        MythicPendingDeliveries store = new MythicPendingDeliveries(file, LOGGER);
        MythicPendingDeliveries.Delivery summoner = MythicPendingDeliveries.Delivery.summoner(player,
            MythicCulture.NORSE);
        MythicPendingDeliveries.Delivery reward = MythicPendingDeliveries.Delivery.reward(player, MythicCulture.GREEK,
            2, 1125, List.of(new MythicDungeonSettings.RewardItem(Material.DIAMOND, 4),
                new MythicDungeonSettings.RewardItem(Material.TRIDENT, 1)));
        store.add(summoner);
        store.add(reward);
        store.add(MythicPendingDeliveries.Delivery.summoner(other, MythicCulture.GREEK));

        assertTrue(Files.isRegularFile(file));
        assertFalse(Files.exists(file.resolveSibling(MythicPendingDeliveries.FILE + ".tmp")));

        MythicPendingDeliveries restarted = new MythicPendingDeliveries(file, LOGGER);
        assertEquals(List.of(summoner, reward), restarted.pending(player));
        assertTrue(restarted.take(reward.id()));
        assertFalse(restarted.take(reward.id()));

        MythicPendingDeliveries again = new MythicPendingDeliveries(file, LOGGER);
        assertEquals(List.of(summoner), again.pending(player));
        assertEquals(2, again.size());
    }

    @Test
    void anEscrowedSummonerComesBackAfterACrashButNotAfterADefeat() {
        Path file = folder.resolve(MythicPendingDeliveries.FILE);
        UUID player = UUID.randomUUID();
        UUID crashedBoss = UUID.randomUUID();
        UUID defeatedBoss = UUID.randomUUID();
        UUID removedBoss = UUID.randomUUID();
        MythicPendingDeliveries store = new MythicPendingDeliveries(file, LOGGER);
        MythicPendingDeliveries.Delivery crashed = MythicPendingDeliveries.Delivery.summoner(player,
            MythicCulture.GREEK);
        store.escrow(crashedBoss, crashed);
        store.escrow(defeatedBoss, MythicPendingDeliveries.Delivery.summoner(player, MythicCulture.NORSE));
        store.escrow(removedBoss, MythicPendingDeliveries.Delivery.summoner(player, MythicCulture.ANDEAN));
        // en garantia no se entrega: la pelea sigue
        assertTrue(store.pending(player).isEmpty());
        assertEquals(3, store.escrowed());
        // la derrota y la baja sin derrota terminan la garantia (el modulo descarta o entrega lo que devuelve)
        assertTrue(store.settle(defeatedBoss).isPresent());
        assertTrue(store.settle(removedBoss).isPresent());
        assertTrue(store.settle(removedBoss).isEmpty());

        // crash: nadie llamo settle; al arrancar de nuevo el Invocador queda para entregar, una sola vez
        MythicPendingDeliveries restarted = new MythicPendingDeliveries(file, LOGGER);
        assertEquals(List.of(crashed), restarted.pending(player));
        assertEquals(0, restarted.escrowed());
        assertTrue(restarted.take(crashed.id()));
        assertTrue(new MythicPendingDeliveries(file, LOGGER).pending(player).isEmpty());
    }

    @Test
    void skipsBrokenEntriesInsteadOfLosingTheRest() throws Exception {
        Path file = folder.resolve(MythicPendingDeliveries.FILE);
        UUID player = UUID.randomUUID();
        UUID good = UUID.randomUUID();
        Files.writeString(file, "pending:\n"
            + "  " + UUID.randomUUID() + ":\n    player: " + player + "\n    kind: SUMMONER\n    culture: atlantis\n"
            + "  " + good + ":\n    player: " + player + "\n    kind: SUMMONER\n    culture: greek\n");

        MythicPendingDeliveries store = new MythicPendingDeliveries(file, LOGGER);

        assertEquals(List.of(new MythicPendingDeliveries.Delivery(good, player, MythicPendingDeliveries.Kind.SUMMONER,
            MythicCulture.GREEK, 0, 0, List.of())), store.pending(player));
    }
}
