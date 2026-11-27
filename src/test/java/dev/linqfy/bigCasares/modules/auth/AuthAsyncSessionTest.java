package dev.linqfy.bigCasares.modules.auth;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthAsyncSessionTest {
    @Test
    void asynchronousResultIsAppliedOnlyToItsOriginalOnlineSession() {
        UUID player = UUID.randomUUID();
        PlayerAuthState first = new PlayerAuthState(player, "Player", null, "address", 0L, null);
        PlayerAuthState rejoined = new PlayerAuthState(player, "Player", null, "address", 1L, null);
        AtomicInteger calls = new AtomicInteger();
        AuthModule.applyIfCurrent(first, rejoined, true, ignored -> calls.incrementAndGet());
        AuthModule.applyIfCurrent(first, first, false, ignored -> calls.incrementAndGet());
        AuthModule.applyIfCurrent(first, null, true, ignored -> calls.incrementAndGet());
        assertEquals(0, calls.get());
        AuthModule.applyIfCurrent(first, first, true, ignored -> calls.incrementAndGet());
        assertEquals(1, calls.get());
    }
}
