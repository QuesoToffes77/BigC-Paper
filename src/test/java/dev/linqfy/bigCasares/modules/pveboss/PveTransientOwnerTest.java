package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PveTransientOwnerTest {

    @Test
    void closesOwnedResourcesOnceInReverseAcquisitionOrder() {
        PveTransientOwner owner = new PveTransientOwner();
        List<String> cleanupOrder = new ArrayList<>();

        owner.own("display", "projectile display", () -> cleanupOrder.add("display"));
        owner.own("task", "projectile task", () -> cleanupOrder.add("task"));

        assertTrue(owner.close().isEmpty());
        assertTrue(owner.close().isEmpty());
        assertEquals(List.of("task", "display"), cleanupOrder);
        assertEquals(0, owner.ownedCount());
    }

    @Test
    void closeContinuesAfterCleanupFailure() {
        PveTransientOwner owner = new PveTransientOwner();
        List<String> cleanupOrder = new ArrayList<>();

        owner.own("display", "projectile display", () -> cleanupOrder.add("display"));
        owner.own("task", "projectile task", () -> {
            cleanupOrder.add("task");
            throw new IllegalStateException("cancel failed");
        });

        List<PveTransientCleanupFailure> failures = owner.close();

        assertEquals(List.of("task", "display"), cleanupOrder);
        assertEquals(1, failures.size());
        assertEquals("projectile task", failures.getFirst().resource());
        assertEquals("cancel failed", failures.getFirst().cause().getMessage());
    }

    @Test
    void releasedResourceIsCleanedOnlyOnce() {
        PveTransientOwner owner = new PveTransientOwner();
        AtomicInteger removals = new AtomicInteger();

        owner.own("clone", "clone", removals::incrementAndGet);

        assertTrue(owner.release("clone"));
        assertFalse(owner.release("clone"));
        assertTrue(owner.close().isEmpty());
        assertEquals(1, removals.get());
    }

    @Test
    void forgottenCompletedTaskIsNotCancelledDuringClose() {
        PveTransientOwner owner = new PveTransientOwner();
        AtomicInteger cancellations = new AtomicInteger();

        owner.own("task", "completed task", cancellations::incrementAndGet);

        assertTrue(owner.forget("task"));
        assertTrue(owner.close().isEmpty());
        assertEquals(0, cancellations.get());
    }

    @Test
    void guardedCallbackCannotMutateAfterClose() {
        PveTransientOwner owner = new PveTransientOwner();
        AtomicInteger mutations = new AtomicInteger();
        Runnable guarded = owner.guard(mutations::incrementAndGet);

        guarded.run();
        owner.close();
        guarded.run();

        assertEquals(1, mutations.get());
        assertFalse(owner.isActive());
    }

    @Test
    void resourceAcquiredAfterCloseIsRemovedImmediately() {
        PveTransientOwner owner = new PveTransientOwner();
        AtomicInteger removals = new AtomicInteger();

        owner.close();
        owner.own("late-clone", "late clone", removals::incrementAndGet);

        assertEquals(1, removals.get());
        assertEquals(0, owner.ownedCount());
    }
}
