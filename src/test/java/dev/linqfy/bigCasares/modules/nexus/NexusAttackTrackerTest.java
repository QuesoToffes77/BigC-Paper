package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class NexusAttackTrackerTest {
    @Test
    void remainsUnderAttackWhileAtLeastOneRegisteredAttackerIsActive() {
        NexusAttackTracker tracker = new NexusAttackTracker();
        NexusId nexus = NexusId.random();
        UUID dead = UUID.randomUUID();
        UUID nearby = UUID.randomUUID();
        tracker.register(nexus, dead);
        tracker.register(nexus, nearby);

        assertTrue(tracker.retain(nexus, Set.of(nearby)::contains));
        assertFalse(tracker.retain(nexus, ignored -> false));
    }
}
