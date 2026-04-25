package dev.linqfy.bigCasares.modules.customcrossbow;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoShardCooldownServiceTest {

    @Test
    void blocksEchoShardShotsUntilCooldownExpires() {
        EchoShardCooldownService service = new EchoShardCooldownService(100);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertTrue(service.tryUse(playerId, 200));
        assertFalse(service.tryUse(playerId, 250));
        assertEquals(50, service.remainingTicks(playerId, 250));
        assertTrue(service.tryUse(playerId, 300));
    }
}
