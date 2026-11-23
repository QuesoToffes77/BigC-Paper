package dev.linqfy.bigCasares.modules.forex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ForexBankBridgeTest {
    @Test
    void workerWaitsForMainThreadAndJournalIntentPrecedesVault(@TempDir Path dir) throws Exception {
        Thread owner = Thread.currentThread();
        Path journalPath = dir.resolve("transfers.log");
        AtomicInteger calls = new AtomicInteger();
        ForexBank delegate = new ForexBank() {
            public long balance(UUID id) { return 100; }
            public boolean withdraw(UUID id, long amount) {
                assertSame(owner, Thread.currentThread());
                try { assertTrue(Files.readString(journalPath).contains("INTENT")); }
                catch (java.io.IOException failure) { throw new IllegalStateException(failure); }
                calls.incrementAndGet(); return true;
            }
            public boolean deposit(UUID id, long amount) { throw new AssertionError(); }
        };
        ForexBankBridge bridge = new ForexBankBridge(delegate, () -> Thread.currentThread() == owner);
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath);
        CompletableFuture<Boolean> transfer = CompletableFuture.supplyAsync(() -> journal.withdraw(UUID.randomUUID(), 20));
        waitFor(() -> bridge.pendingCount() == 1);
        assertEquals(0, calls.get());
        bridge.pump();
        assertTrue(transfer.get(5, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        bridge.close();
    }

    @Test
    void closingRejectsQueuedVaultCallAndPreservesUncertainJournal(@TempDir Path dir) throws Exception {
        Thread owner = Thread.currentThread();
        Path journalPath = dir.resolve("transfers.log");
        AtomicInteger calls = new AtomicInteger();
        ForexBank delegate = new ForexBank() {
            public long balance(UUID id) { return 100; }
            public boolean withdraw(UUID id, long amount) { calls.incrementAndGet(); return true; }
            public boolean deposit(UUID id, long amount) { calls.incrementAndGet(); return true; }
        };
        ForexBankBridge bridge = new ForexBankBridge(delegate, () -> Thread.currentThread() == owner);
        JournaledForexBank journal = new JournaledForexBank(bridge, journalPath);
        CompletableFuture<Boolean> transfer = CompletableFuture.supplyAsync(() -> journal.withdraw(UUID.randomUUID(), 20));
        waitFor(() -> bridge.pendingCount() == 1);
        bridge.close();
        assertThrows(java.util.concurrent.ExecutionException.class, () -> transfer.get(5, TimeUnit.SECONDS));
        assertEquals(0, calls.get());
        assertTrue(journal.paused());
        assertTrue(Files.exists(journalPath));
        assertTrue(new JournaledForexBank(delegate, journalPath).paused());
    }

    private static void waitFor(java.util.function.BooleanSupplier condition) throws Exception {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= until) fail("timed out");
            Thread.sleep(1);
        }
    }
}
