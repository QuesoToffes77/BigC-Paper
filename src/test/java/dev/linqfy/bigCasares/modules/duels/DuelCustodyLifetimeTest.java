package dev.linqfy.bigCasares.modules.duels;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DuelCustodyLifetimeTest {
    @Test void unregisterWaitsUntilAllReentrantTransfersFinish() {
        var lifetime = new DuelCustodyLifetime();
        var calls = new AtomicInteger();
        lifetime.enter();
        lifetime.enter();
        lifetime.closeWhenIdle(calls::incrementAndGet);
        assertEquals(0, calls.get());
        lifetime.leave();
        assertEquals(0, calls.get());
        lifetime.leave();
        assertEquals(1, calls.get());
    }

    @Test void idleUnregisterRunsImmediately() {
        var lifetime = new DuelCustodyLifetime();
        var calls = new AtomicInteger();
        lifetime.closeWhenIdle(calls::incrementAndGet);
        assertEquals(1, calls.get());
    }
}
