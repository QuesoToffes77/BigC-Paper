package dev.linqfy.bigCasares.modules.auth;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthDiscordDecisionTest {
    @Test
    void decisionRequiresCurrentOnlineSessionRequestExpiryAndLinkedAccount() {
        UUID id = UUID.randomUUID();
        PlayerAuthState expected = new PlayerAuthState(id, "Player", null, "address", 0L, null);
        PlayerAuthState rejoined = new PlayerAuthState(id, "Player", null, "address", 1L, null);
        expected.discordRequestId = "request";
        expected.discordExpiresAt = 100L;
        assertTrue(AuthModule.validDiscordDecision(expected, expected, true, "request", 100L, Optional.of(7L), 7L));
        assertFalse(AuthModule.validDiscordDecision(expected, rejoined, true, "request", 99L, Optional.of(7L), 7L));
        assertFalse(AuthModule.validDiscordDecision(expected, expected, false, "request", 99L, Optional.of(7L), 7L));
        assertFalse(AuthModule.validDiscordDecision(expected, expected, true, "other", 99L, Optional.of(7L), 7L));
        assertFalse(AuthModule.validDiscordDecision(expected, expected, true, "request", 101L, Optional.of(7L), 7L));
        assertFalse(AuthModule.validDiscordDecision(expected, expected, true, "request", 99L, Optional.of(8L), 7L));
        assertFalse(AuthModule.validDiscordDecision(expected, expected, true, "request", 99L, Optional.empty(), 7L));
        expected.authenticated = true;
        assertFalse(AuthModule.validDiscordDecision(expected, expected, true, "request", 99L, Optional.of(7L), 7L));
    }
}
