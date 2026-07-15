package dev.linqfy.bigCasares.modules.moderation;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RareItemGainTrackerTest {

    @Test
    void signalsOnlyUnexplainedGainsAfterTheInitialSnapshot() {
        RareItemGainTracker tracker = new RareItemGainTracker(Set.of("ELYTRA"));
        UUID player = UUID.randomUUID();
        Instant now = Instant.parse("2026-07-15T12:00:00Z");

        assertTrue(tracker.sample(now, player, "Player", Map.of("ELYTRA", 1)).isEmpty());
        assertEquals(25, tracker.sample(now.plusSeconds(1), player, "Player", Map.of("ELYTRA", 2))
            .orElseThrow().points());
        tracker.markExplained(player);
        assertTrue(tracker.sample(now.plusSeconds(2), player, "Player", Map.of("ELYTRA", 3)).isEmpty());
    }
}
