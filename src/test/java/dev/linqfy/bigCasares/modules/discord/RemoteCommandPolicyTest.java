package dev.linqfy.bigCasares.modules.discord;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RemoteCommandPolicyTest {

    @Test
    void ordinaryCommandsExecuteWithoutConfirmation() {
        assertFalse(new RemoteCommandPolicy(Duration.ofSeconds(30)).requiresConfirmation("say hola mundo"));
    }

    @Test
    void destructiveAndAuthorityChangingCommandsRequireConfirmation() {
        RemoteCommandPolicy policy = new RemoteCommandPolicy(Duration.ofSeconds(30));

        assertTrue(policy.requiresConfirmation("/stop"));
        assertTrue(policy.requiresConfirmation("minecraft:execute as @e run kill @s"));
        assertTrue(policy.requiresConfirmation("lp user Pepe permission set *"));
        assertTrue(policy.requiresConfirmation("whitelist off"));
    }

    @Test
    void confirmationsAreUserBoundAndExpire() {
        RemoteCommandPolicy policy = new RemoteCommandPolicy(Duration.ofSeconds(30));
        Instant now = Instant.parse("2026-07-15T12:00:00Z");
        String id = policy.createConfirmation("42", "stop", now);

        assertFalse(policy.confirm(id, "99", now.plusSeconds(1)).isPresent());
        assertFalse(policy.confirm(id, "42", now.plusSeconds(31)).isPresent());
        String valid = policy.createConfirmation("42", "stop", now);
        assertTrue(policy.confirm(valid, "42", now.plusSeconds(29)).isPresent());
        assertFalse(policy.confirm(valid, "42", now.plusSeconds(29)).isPresent());
    }
}
