package dev.linqfy.bigCasares.modules.moderation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

final class AuditRedactorTest {

    @Test
    void redactsPasswordArgumentsTokensAndWebhookUrls() {
        String input = "/login hunter2 token=abc123 https://discord.com/api/webhooks/1/secret";

        String redacted = AuditRedactor.redact(input);

        assertFalse(redacted.contains("hunter2"));
        assertFalse(redacted.contains("abc123"));
        assertFalse(redacted.contains("secret"));
        assertEquals("/login [REDACTADO]", redacted);
        assertEquals("token=[REDACTADO] [WEBHOOK REDACTADO]",
            AuditRedactor.redact("token=abc123 https://discord.com/api/webhooks/1/secret"));
        assertEquals("/changepass [REDACTADO]", AuditRedactor.redact("/changepass sample"));
        assertEquals("login completed successfully", AuditRedactor.redact("login completed successfully"));
    }
}
