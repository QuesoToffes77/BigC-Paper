package dev.linqfy.bigCasares.modules.auth;

import org.junit.jupiter.api.Test;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthHashExecutorTest {
    @Test
    void boundsWorkersAndQueueRejectsOnceAndShutdownDoesNotWait() throws Exception {
        AuthHashExecutor executor = new AuthHashExecutor(1, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger rejected = new AtomicInteger();
        try {
            assertTrue(executor.submit(() -> {
                started.countDown();
                try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }, rejected::incrementAndGet));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(executor.submit(() -> { }, rejected::incrementAndGet));
            PlayerAuthState state = new PlayerAuthState(UUID.randomUUID(), "Player", null, "address", 0L, null);
            state.busy = true;
            assertFalse(AuthModule.submitHash(executor, state, () -> { }, rejected::incrementAndGet));
            assertFalse(state.busy, "a rejected request must allow another attempt");
            assertEquals(1, rejected.get());
        } finally {
            executor.close();
            release.countDown();
        }
        assertFalse(executor.submit(() -> { }, rejected::incrementAndGet));
        assertEquals(2, rejected.get());
    }

    @Test
    void retiringGenerationDoesNotWaitForRunningHash() throws Exception {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        AuthHashExecutor hashes = new AuthHashExecutor(1, 1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var closer = Executors.newSingleThreadExecutor();
        try {
            assertTrue(hashes.submit(AuthModule.guardedHash(scope, () -> {
                started.countDown();
                try { release.await(); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }), () -> { }));
            assertTrue(started.await(5, TimeUnit.SECONDS));
            closer.submit(scope::close).get(2, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            hashes.close();
            closer.shutdownNow();
        }
    }
}
