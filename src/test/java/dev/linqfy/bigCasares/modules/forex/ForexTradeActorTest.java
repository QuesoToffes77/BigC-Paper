package dev.linqfy.bigCasares.modules.forex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class ForexTradeActorTest {
    @Test
    void workerTradeSettlesBeforeMainThreadCompletion(@TempDir Path dir) throws Exception {
        Thread main = Thread.currentThread();
        UUID player = UUID.randomUUID();
        FakeBank money = new FakeBank().give(player, 1_000);
        ForexBankBridge bridge = new ForexBankBridge(money, () -> Thread.currentThread() == main);
        Path marketPath = dir.resolve("market.yml"), journalPath = dir.resolve("transfers.log");
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath, marketPath);
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), journal, new Random(1), () -> 1L);
        ForexStore store = new ForexStore(marketPath.toFile(), Logger.getAnonymousLogger());
        ForexAsyncWriter writer = new ForexAsyncWriter(marketPath);
        AtomicReference<TradeReceipt> receipt = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        boolean[] completed = {false};
        try {
            writer.submit(() -> receipt.set(ForexTradeActor.settle(journal, market, store, "buy",
                () -> market.buyEmeralds(player, 200))), error -> { failure.set(error); completed[0] = true; });
            waitFor(() -> bridge.pendingCount() > 0);
            assertTrue(Files.readString(journalPath).contains("INTENT"));
            assertEquals(1_000, money.balance(player));
            assertFalse(completed[0]);
            bridge.pump();
            waitFor(() -> { writer.poll(); return completed[0]; });
            assertNull(failure.get());
            assertNotNull(receipt.get());
            assertEquals(800, money.balance(player));
            assertTrue(Files.isRegularFile(marketPath));
            assertFalse(Files.exists(journalPath));
            ForexMarket reloaded = new ForexMarket(ForexSettings.defaults(), money, new Random(2), () -> 1L);
            store.load(reloaded);
            assertEquals(market.emeralds(player), reloaded.emeralds(player), 1e-9);
        } finally { bridge.close(); writer.close(); }
    }

    @Test
    void shutdownBeforeVaultLeavesJournalAndPreventsReplay(@TempDir Path dir) throws Exception {
        Thread main = Thread.currentThread();
        UUID player = UUID.randomUUID();
        FakeBank money = new FakeBank().give(player, 1_000);
        ForexBankBridge bridge = new ForexBankBridge(money, () -> Thread.currentThread() == main);
        Path marketPath = dir.resolve("market.yml"), journalPath = dir.resolve("transfers.log");
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath, marketPath);
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), journal, new Random(1), () -> 1L);
        ForexStore store = new ForexStore(marketPath.toFile(), Logger.getAnonymousLogger());
        ForexAsyncWriter writer = new ForexAsyncWriter(marketPath);
        writer.submit(() -> ForexTradeActor.settle(journal, market, store, "buy", () -> market.buyEmeralds(player, 200)),
            ignored -> fail("closed runtime must not notify"));
        waitFor(() -> bridge.pendingCount() > 0);
        bridge.close();
        writer.close();
        assertTrue(Files.exists(journalPath));
        assertTrue(new JournaledForexBank(money, journalPath, marketPath).paused());
        assertEquals(1_000, money.balance(player));
        waitForLeaseRelease(marketPath);
        writer.poll();
    }

    @Test
    void shutdownBetweenTradeTransfersKeepsEvidenceOfTheFirst(@TempDir Path dir) throws Exception {
        Thread main = Thread.currentThread();
        UUID buyer = UUID.randomUUID(), creator = UUID.randomUUID();
        FakeBank money = new FakeBank().give(buyer, 1_000);
        ForexBankBridge bridge = new ForexBankBridge(money, () -> Thread.currentThread() == main);
        Path marketPath = dir.resolve("market.yml"), journalPath = dir.resolve("transfers.log");
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath, marketPath);
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), journal, new Random(1), () -> 1L);
        market.restoreCoin(new Shitcoin("MOON", "Moon", creator, 1, 0.04, 0));
        ForexStore store = new ForexStore(marketPath.toFile(), Logger.getAnonymousLogger());
        ForexAsyncWriter writer = new ForexAsyncWriter(marketPath);
        writer.submit(() -> ForexTradeActor.settle(journal, market, store, "buy MOON",
            () -> market.buy(buyer, "MOON", 200)), ignored -> fail("closed runtime must not notify"));
        waitFor(() -> bridge.pendingCount() > 0);
        bridge.pump();
        waitFor(() -> bridge.pendingCount() > 0);
        bridge.close();
        writer.close();
        String evidence = Files.readString(journalPath);
        assertTrue(evidence.contains("SUCCEEDED"));
        assertEquals(2, evidence.split("INTENT ", -1).length - 1);
        assertEquals(800, money.balance(buyer));
        assertEquals(0, money.balance(creator));
        assertTrue(new JournaledForexBank(money, journalPath, marketPath).paused());
        waitForLeaseRelease(marketPath);
        writer.poll();
    }

    @Test
    void bulkPayoutRunsAtMostOneVaultCallPerMainThreadPump(@TempDir Path dir) throws Exception {
        Thread main = Thread.currentThread();
        UUID creator = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        FakeBank money = new FakeBank();
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        ForexBank delegate = new ForexBank() {
            public long balance(UUID id) { assertSame(main, Thread.currentThread()); return money.balance(id); }
            public boolean withdraw(UUID id, long amount) { assertSame(main, Thread.currentThread()); calls.incrementAndGet(); return money.withdraw(id, amount); }
            public boolean deposit(UUID id, long amount) { assertSame(main, Thread.currentThread()); calls.incrementAndGet(); return money.deposit(id, amount); }
        };
        ForexBankBridge bridge = new ForexBankBridge(delegate, () -> Thread.currentThread() == main);
        Path marketPath = dir.resolve("market.yml"), journalPath = dir.resolve("transfers.log");
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath, marketPath);
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), journal, new Random(1), () -> 1L);
        Shitcoin coin = new Shitcoin("MOON", "Moon", creator, 1, 0.04, 0);
        coin.pool(10_000); coin.supply(200); coin.restoreHolding(first, 100, 0, 0); coin.restoreHolding(second, 100, 0, 0);
        market.restoreCoin(coin);
        ForexStore store = new ForexStore(marketPath.toFile(), Logger.getAnonymousLogger());
        ForexAsyncWriter writer = new ForexAsyncWriter(marketPath);
        AtomicReference<RugPull> result = new AtomicReference<>();
        boolean[] completed = {false};
        try {
            writer.submit(() -> result.set(ForexTradeActor.settle(journal, market, store, "rug",
                () -> market.rugPull(creator, "MOON"))), error -> { assertNull(error); completed[0] = true; });
            for (int expected = 1; expected <= 3; expected++) {
                waitFor(() -> bridge.pendingCount() > 0);
                bridge.pump();
                assertEquals(expected, calls.get());
                assertFalse(completed[0]);
            }
            waitFor(() -> { writer.poll(); return completed[0]; });
            assertNotNull(result.get());
            assertEquals(3, calls.get());
            assertFalse(Files.exists(journalPath));
        } finally { bridge.close(); writer.close(); }
    }

    @Test
    void periodicDividendsUseTheBridgeAndPersistBeforeAnnouncing(@TempDir Path dir) throws Exception {
        Thread main = Thread.currentThread();
        UUID holder = UUID.randomUUID();
        FakeBank money = new FakeBank();
        ForexBankBridge bridge = new ForexBankBridge(money, () -> Thread.currentThread() == main);
        Path marketPath = dir.resolve("market.yml"), journalPath = dir.resolve("transfers.log");
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath, marketPath);
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), journal, new Random(1), () -> 4_000_000L);
        Shitcoin coin = new Shitcoin("MOON", "Moon", UUID.randomUUID(), 0, 0.04, 0);
        coin.pool(10_000); coin.supply(100); coin.dividendPot(500); coin.restoreHolding(holder, 100, 100, 0);
        market.restoreCoin(coin);
        ForexStore store = new ForexStore(marketPath.toFile(), Logger.getAnonymousLogger());
        ForexAsyncWriter writer = new ForexAsyncWriter(marketPath);
        AtomicReference<java.util.List<MarketEvent>> events = new AtomicReference<>();
        boolean[] completed = {false};
        try {
            writer.submit(() -> events.set(ForexTradeActor.tick(journal, market, store)),
                error -> { assertNull(error); completed[0] = true; });
            waitFor(() -> bridge.pendingCount() > 0);
            assertTrue(Files.readString(journalPath).contains("INTENT"));
            assertEquals(0, money.balance(holder));
            bridge.pump();
            waitFor(() -> { writer.poll(); return completed[0]; });
            assertEquals(500, money.balance(holder));
            assertTrue(events.get().stream().anyMatch(MarketEvent.Dividends.class::isInstance));
            assertTrue(Files.isRegularFile(marketPath));
            assertFalse(Files.exists(journalPath));
        } finally { bridge.close(); writer.close(); }
    }

    private static void waitForLeaseRelease(Path path) throws Exception {
        waitFor(() -> {
            try (ForexAsyncWriter ignored = new ForexAsyncWriter(path)) { return true; }
            catch (IllegalStateException busy) { return false; }
        });
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= until) fail("timed out");
            Thread.sleep(1);
        }
    }
}
