package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackReloadMessageFormatterTest {

    @Test
    void failureKeepsPreviousPackClaimAndHidesExceptionDetails() {
        PackReloadResult result = new PackReloadResult(
            UUID.randomUUID(), PackReloadStatus.FAILED, PackReloadPhase.BUILD,
            Duration.ZERO, null, null, List.of(), new IllegalStateException("secret path"));

        String message = PackReloadMessageFormatter.format(result);

        assertTrue(message.startsWith("§c"));
        assertTrue(message.contains("pack anterior sigue activo"));
        assertFalse(message.contains("secret path"));
    }

    @Test
    void noOpExplicitlySaysPackWasNotResent() {
        PackReloadResult result = new PackReloadResult(
            UUID.randomUUID(), PackReloadStatus.NO_OP, null,
            Duration.ZERO, null, null, List.of(), null);

        String message = PackReloadMessageFormatter.format(result);

        assertTrue(message.startsWith("§e"));
        assertTrue(message.contains("no se reenvió"));
    }
}
