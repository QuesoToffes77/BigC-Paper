package dev.linqfy.bigCasares.communication;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialCommandSanitizerTest {
    @Test
    void hidesArgumentsForAliasesAndNamespacedCommands() {
        assertEquals("/login [redacted]", CredentialCommandSanitizer.redact("/login sample"));
        assertEquals("/bigcasares:REG [redacted]", CredentialCommandSanitizer.redact("/bigcasares:REG sample sample"));
        assertEquals("auth [redacted]", CredentialCommandSanitizer.redact("auth password sample"));
        assertEquals(" /CUENTA [redacted]", CredentialCommandSanitizer.redact(" /CUENTA password sample"));
        assertTrue(CredentialCommandSanitizer.isCredentialCommand("/bigcasares:l sample"));
    }

    @Test
    void leavesOrdinaryAndNearMatchCommandsByteIdentical() {
        for (String command : new String[] {"/spawn x", "/login-help sample", "/author sample", "  /tell a hi"}) {
            assertFalse(CredentialCommandSanitizer.isCredentialCommand(command));
            assertEquals(command, CredentialCommandSanitizer.redact(command));
        }
    }
}
