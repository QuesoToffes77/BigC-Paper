package dev.linqfy.bigCasares.module.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeRegistrationScopeTest {

    @Test
    void bindsOwnerAndGuardedCallbacksToGeneration() {
        RuntimeGeneration generation = new RuntimeGeneration(41);
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope(generation, "shop-system");
        AtomicInteger mutations = new AtomicInteger();
        Runnable callback = scope.guard(mutations::incrementAndGet);

        callback.run();
        generation.retire();
        callback.run();

        assertEquals(41, scope.generation().id());
        assertEquals("shop-system", scope.ownerId());
        assertEquals(1, mutations.get());
        assertFalse(generation.isActive());
    }

    @Test
    void closesNamedActionsOnceInReverseRegistrationOrder() {
        List<String> closed = new ArrayList<>();
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        scope.register("listeners", () -> closed.add("listeners"));
        scope.register("tasks", () -> closed.add("tasks"));

        RuntimeCleanupReport first = scope.close();
        RuntimeCleanupReport second = scope.close();

        assertEquals(List.of("tasks", "listeners"), closed);
        assertEquals(List.of("tasks", "listeners"), first.outcomes().stream()
            .map(RuntimeCleanupOutcome::resourceId)
            .toList());
        assertFalse(first.alreadyClosed());
        assertTrue(second.alreadyClosed());
        assertTrue(second.outcomes().isEmpty());
    }

    @Test
    void attemptsEveryActionAndAggregatesFailures() {
        List<String> closed = new ArrayList<>();
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        scope.register("listeners", () -> closed.add("listeners"));
        scope.register("tasks", () -> {
            closed.add("tasks");
            throw new IllegalStateException("scheduler unavailable");
        });
        scope.register("transport", () -> closed.add("transport"));

        RuntimeCleanupReport report = scope.close();

        assertEquals(List.of("transport", "tasks", "listeners"), closed);
        assertTrue(report.hasFailures());
        assertEquals(List.of("tasks"), report.failures().stream()
            .map(RuntimeCleanupOutcome::resourceId)
            .toList());
        assertEquals("scheduler unavailable", report.failures().getFirst().failure().getMessage());
    }

    @Test
    void rejectsDuplicateNamesAndRegistrationAfterClose() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        scope.register("tasks", () -> { });

        assertThrows(IllegalArgumentException.class, () -> scope.register("tasks", () -> { }));

        scope.close();

        assertThrows(IllegalStateException.class, () -> scope.register("listeners", () -> { }));
    }

    @Test
    void forgetDetachesCompletedResourceWithoutRunningCleanup() {
        List<String> closed = new ArrayList<>();
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        scope.register("one-shot-task", () -> closed.add("task"));

        assertTrue(scope.forget("one-shot-task"));
        assertFalse(scope.forget("one-shot-task"));
        scope.close();

        assertTrue(closed.isEmpty());
        assertFalse(scope.forget("one-shot-task"));
    }

    @Test
    void retiresGenerationBeforeCleanupStarts() {
        RuntimeGeneration generation = new RuntimeGeneration(7);
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope(generation, "missions");
        List<Boolean> activeDuringCleanup = new ArrayList<>();
        scope.register("listener", () -> activeDuringCleanup.add(generation.isActive()));

        scope.close();

        assertEquals(List.of(false), activeDuringCleanup);
    }

    @Test
    void retireWaitsForRunningGuardedCallbackAndOrdersLaterCallbacksAfterRetirement() throws Exception {
        RuntimeGeneration generation = new RuntimeGeneration(8);
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        CountDownLatch retireReturned = new CountDownLatch(1);
        AtomicInteger mutations = new AtomicInteger();
        AtomicReference<Boolean> retireResult = new AtomicReference<>();
        Runnable guarded = generation.guard(() -> {
            callbackEntered.countDown();
            await(releaseCallback);
            mutations.incrementAndGet();
        });
        Thread callbackThread = new Thread(guarded);
        Thread retireThread = new Thread(() -> {
            retireResult.set(generation.retire());
            retireReturned.countDown();
        });

        callbackThread.start();
        assertTrue(callbackEntered.await(5, TimeUnit.SECONDS));
        retireThread.start();
        boolean retireReturnedWhileCallbackWasRunning;
        try {
            retireReturnedWhileCallbackWasRunning = retireReturned.await(100, TimeUnit.MILLISECONDS);
        } finally {
            releaseCallback.countDown();
        }
        assertTrue(retireReturned.await(5, TimeUnit.SECONDS));
        guarded.run();
        callbackThread.join(5000);
        retireThread.join(5000);

        assertFalse(retireReturnedWhileCallbackWasRunning);
        assertEquals(Boolean.TRUE, retireResult.get());
        assertEquals(1, mutations.get());
        assertFalse(callbackThread.isAlive());
        assertFalse(retireThread.isAlive());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
