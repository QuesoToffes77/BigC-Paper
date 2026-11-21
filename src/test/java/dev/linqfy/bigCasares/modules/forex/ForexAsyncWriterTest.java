package dev.linqfy.bigCasares.modules.forex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class ForexAsyncWriterTest {
    @Test
    void serializesAndDeliversCompletionOnlyWhenPolled(@TempDir Path dir) throws Exception {
        ForexAsyncWriter writer = new ForexAsyncWriter(dir.resolve("market.yml"));
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> calls = new ArrayList<>();
        try {
            writer.submit(() -> { entered.countDown(); try { release.await(); } catch (InterruptedException e) { throw new RuntimeException(e); } },
                failure -> calls.add(failure == null ? "saved" : "failed"));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertTrue(writer.busy());
            assertThrows(IllegalStateException.class, () -> writer.submit(() -> {}, ignored -> {}));
            assertTrue(calls.isEmpty());
            release.countDown();
            assertTrue(calls.isEmpty());
            waitFor(() -> { writer.poll(); return !writer.busy(); });
            assertEquals(List.of("saved"), calls);
            assertFalse(writer.busy());
        } finally { release.countDown(); writer.close(); }
    }

    @Test
    void failedWriteReportsFailureAndNoSuccess(@TempDir Path dir) throws Exception {
        ForexAsyncWriter writer = new ForexAsyncWriter(dir.resolve("market.yml"));
        List<Throwable> failures = new ArrayList<>();
        try {
            writer.submit(() -> { throw new IllegalStateException("disk"); }, failures::add);
            waitFor(() -> { writer.poll(); return !writer.busy(); });
            assertEquals(1, failures.size());
            assertInstanceOf(IllegalStateException.class, failures.getFirst());
        } finally { writer.close(); }
    }

    @Test
    void closeKeepsLeaseUntilOutstandingWriteActuallyEnds(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("market.yml");
        ForexAsyncWriter writer = new ForexAsyncWriter(path);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            writer.submit(() -> { entered.countDown(); try { release.await(); } catch (InterruptedException e) { throw new RuntimeException(e); } }, ignored -> fail());
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            writer.close();
            assertTrue(ForexAsyncWriter.leased(path));
            assertThrows(IllegalStateException.class, () -> new ForexAsyncWriter(path));
        } finally { release.countDown(); }
        waitFor(() -> {
            try (ForexAsyncWriter next = new ForexAsyncWriter(path)) { return !next.busy(); }
            catch (IllegalStateException busy) { return false; }
        });
    }

    @Test
    void shutdownKeepsJournalAndFencesReloadUntilSettlementFinishes(@TempDir Path dir) throws Exception {
        Path marketPath = dir.resolve("market.yml");
        Path journalPath = dir.resolve("transfers.log");
        java.util.UUID id = java.util.UUID.randomUUID();
        FakeBank delegate = new FakeBank().give(id, 100);
        JournaledForexBank journal = new JournaledForexBank(delegate, journalPath, marketPath);
        assertTrue(journal.withdraw(id, 20));
        ForexStore store = new ForexStore(marketPath.toFile(), java.util.logging.Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), delegate, new ScriptedRandom(), () -> 0L);
        ForexStore.Snapshot snapshot = ForexStore.capture(market);
        ForexAsyncWriter writer = new ForexAsyncWriter(marketPath);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            writer.submit(() -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException failure) { throw new IllegalStateException(failure); }
                ForexSettlement.persist(store, snapshot, journal);
            }, ignored -> fail("closed writer must not notify"));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            writer.close();
            assertTrue(new JournaledForexBank(delegate, journalPath, marketPath).paused());
            assertThrows(IllegalStateException.class, () -> new ForexAsyncWriter(marketPath));
        } finally { release.countDown(); }
        waitFor(() -> {
            try (ForexAsyncWriter next = new ForexAsyncWriter(marketPath)) { return true; }
            catch (IllegalStateException busy) { return false; }
        });
        writer.poll();
        assertFalse(java.nio.file.Files.exists(journalPath));
        assertTrue(java.nio.file.Files.isRegularFile(marketPath));
        assertEquals(80, delegate.balance(id));
    }

    @Test
    void failedSnapshotAfterVaultKeepsJournalAndPausesOnRestart(@TempDir Path dir) throws Exception {
        Path journalPath = dir.resolve("transfers.log");
        java.util.UUID id = java.util.UUID.randomUUID();
        FakeBank delegate = new FakeBank().give(id, 100);
        JournaledForexBank journal = new JournaledForexBank(delegate, journalPath);
        assertTrue(journal.withdraw(id, 20));
        Path file = java.nio.file.Files.createFile(dir.resolve("not-a-directory"));
        ForexStore store = new ForexStore(file.resolve("market.yml").toFile(), java.util.logging.Logger.getAnonymousLogger());
        ForexMarket market = new ForexMarket(ForexSettings.defaults(), delegate, new ScriptedRandom(), () -> 0L);
        ForexStore.Snapshot snapshot = ForexStore.capture(market);
        ForexAsyncWriter writer = new ForexAsyncWriter(dir.resolve("market.yml"));
        List<Throwable> failures = new ArrayList<>();
        try {
            writer.submit(() -> ForexSettlement.persist(store, snapshot, journal), failures::add);
            waitFor(() -> { writer.poll(); return !writer.busy(); });
            assertEquals(1, failures.size());
            assertNotNull(failures.getFirst());
            assertTrue(journal.paused());
            assertTrue(java.nio.file.Files.exists(journalPath));
            assertTrue(new JournaledForexBank(delegate, journalPath).paused());
            assertEquals(80, delegate.balance(id));
        } finally { writer.close(); }
    }

    @Test
    void fastCompletionAndCloseNeverLeakTheStoreLease(@TempDir Path dir) throws Exception {
        Path path = dir.resolve("market.yml");
        for (int i = 0; i < 100; i++) {
            ForexAsyncWriter writer = new ForexAsyncWriter(path);
            writer.submit(() -> {}, ignored -> {});
            writer.close();
            waitFor(() -> {
                try (ForexAsyncWriter next = new ForexAsyncWriter(path)) { return true; }
                catch (IllegalStateException busy) { return false; }
            });
        }
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) fail("timed out");
            Thread.sleep(1);
        }
    }
}
