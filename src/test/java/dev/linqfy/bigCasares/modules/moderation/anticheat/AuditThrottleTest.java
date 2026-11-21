package dev.linqfy.bigCasares.modules.moderation.anticheat;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuditThrottleTest {
    @Test void repeatedPlayerRuleIsLimitedWhileOtherPlayersAndRulesProceed() {
        var throttle = new AuditThrottle(30, 10);
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        assertTrue(throttle.allow(a, "hover-flight", 100));
        assertFalse(throttle.allow(a, "hover-flight", 110));
        assertTrue(throttle.allow(a, "melee-rate", 110));
        assertTrue(throttle.allow(b, "hover-flight", 110));
        assertTrue(throttle.allow(a, "hover-flight", 130));
    }
    @Test void storageRemainsBounded() {
        var throttle = new AuditThrottle(30, 2);
        UUID first = UUID.randomUUID();
        assertTrue(throttle.allow(first, "x", 1));
        assertTrue(throttle.allow(UUID.randomUUID(), "x", 2));
        assertFalse(throttle.allow(UUID.randomUUID(), "x", 3));
        assertFalse(throttle.allow(first, "x", 4));
        assertEquals(2, throttle.size());
    }
}
