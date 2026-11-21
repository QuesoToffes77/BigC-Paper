package dev.linqfy.bigCasares.modules.forex;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForexStoreTest {

    @Test
    void saveFailureIsReportedToTheCaller(@TempDir Path directory) throws Exception {
        Path notDirectory = Files.createFile(directory.resolve("not-a-directory"));
        ForexStore store = new ForexStore(notDirectory.resolve("market.yml").toFile(), Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), new FakeBank(), new ScriptedRandom(), () -> 0L);
        assertThrows(IllegalStateException.class, () -> store.save(market));
    }

    @Test
    void invalidAccountStopsLoadWithoutChangingTheFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("market.yml");
        String source = "accounts:\n  00000000-0000-0000-0000-000000000001:\n    emeralds: 5\n"
            + "  not-a-uuid:\n    emeralds: 123\n";
        Files.writeString(file, source);
        ForexStore store = new ForexStore(file.toFile(), Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), new FakeBank(), new ScriptedRandom(), () -> 0L);
        assertThrows(IllegalStateException.class, () -> store.load(market));
        assertTrue(market.accounts().isEmpty(), "a partially parsed snapshot must not become live");
        assertEquals(source, Files.readString(file));
    }

    @Test
    void invalidCoinHolderStopsLoadWithoutChangingTheFile(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("market.yml");
        String source = "coins:\n  MOON:\n    creator: 00000000-0000-0000-0000-000000000001\n"
            + "    holders:\n      not-a-uuid:\n        balance: 12\n";
        Files.writeString(file, source);
        ForexStore store = new ForexStore(file.toFile(), Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), new FakeBank(), new ScriptedRandom(), () -> 0L);
        assertThrows(IllegalStateException.class, () -> store.load(market));
        assertEquals(source, Files.readString(file));
    }

    @Test
    void malformedFinancialNumberStopsLoad(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("market.yml");
        String source = "accounts:\n  00000000-0000-0000-0000-000000000001:\n    emeralds: broken\n";
        Files.writeString(file, source);
        ForexStore store = new ForexStore(file.toFile(), Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), new FakeBank(), new ScriptedRandom(), () -> 0L);
        assertThrows(IllegalStateException.class, () -> store.load(market));
        assertEquals(source, Files.readString(file));
    }

    @Test
    void omittedLegacyFieldsStillUseDefaults(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("market.yml");
        Files.writeString(file, "accounts:\n  00000000-0000-0000-0000-000000000001:\n    emeralds: 3.0\n");
        ForexStore store = new ForexStore(file.toFile(), Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), new FakeBank(), new ScriptedRandom(), () -> 0L);
        store.load(market);
        assertEquals(3.0, market.emeralds(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    @Test
    void capturedSnapshotDoesNotChangeWithTheLiveMarket() throws Exception {
        UUID player = UUID.randomUUID();
        FakeBank bank = new FakeBank().give(player, 5_000);
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), bank, new ScriptedRandom(), () -> 0L);
        market.buyEmeralds(player, 1_000);
        ForexStore.Snapshot snapshot = ForexStore.capture(market);
        String before = ForexStore.serialize(snapshot).saveToString();
        market.buyEmeralds(player, 1_000);
        assertEquals(before, ForexStore.serialize(snapshot).saveToString());
    }

    @Test
    void theWholeMarketSurvivesARestart() throws Exception {
        UUID creator = UUID.randomUUID();
        UUID alice = UUID.randomUUID();
        UUID rugger = UUID.randomUUID();
        FakeBank bank = new FakeBank().give(creator, 50_000).give(alice, 50_000).give(rugger, 50_000);
        long[] now = {5_000_000L};
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), bank, new ScriptedRandom(), () -> now[0]);
        market.buyEmeralds(alice, 2_000);
        market.create(creator, "MOON", "Moon Coin");
        market.linkTeam(creator, "MOON", "team-7");
        market.setCreatorFee(creator, "MOON", 0.05);
        market.buy(alice, "MOON", 8_000);
        market.stake(alice, "MOON", 300);
        market.create(rugger, "SCAM", "Scam");
        market.rugPull(rugger, "SCAM");
        market.toggleAura(alice);
        market.tick();

        String saved = ForexStore.serialize(market).saveToString();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(saved);
        ForexMarket restored = new ForexMarket(ForexSettings.defaults(), bank, new ScriptedRandom(), () -> now[0]);
        ForexStore.deserialize(yaml, restored, Logger.getAnonymousLogger());

        Shitcoin before = market.coin("MOON").orElseThrow();
        Shitcoin after = restored.coin("MOON").orElseThrow();
        assertEquals(market.emeralds(alice), restored.emeralds(alice), 1e-9);
        assertEquals(before.pool(), after.pool());
        assertEquals(before.dividendPot(), after.dividendPot());
        assertEquals(before.supply(), after.supply(), 1e-9);
        assertEquals(before.balance(alice), after.balance(alice), 1e-9);
        assertEquals(300, after.staked(alice), 1e-9);
        assertEquals(before.lockedUntil(alice), after.lockedUntil(alice));
        assertEquals(0.05, after.creatorFee(), 1e-12);
        assertEquals("team-7", after.teamId().orElseThrow());
        assertEquals(before.history(), after.history());
        assertEquals(market.price(before), restored.price(after), 1e-12);
        assertEquals(1, restored.rugPulls(rugger));
        assertTrue(restored.auraHidden(alice));
        assertEquals(market.graveyard(), restored.graveyard());
        assertEquals(market.millisToNextCycle(), restored.millisToNextCycle());
    }
}
