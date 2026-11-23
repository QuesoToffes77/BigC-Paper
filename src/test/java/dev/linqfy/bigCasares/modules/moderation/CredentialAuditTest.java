package dev.linqfy.bigCasares.modules.moderation;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CredentialAuditTest {
    @Test
    void sinkReceivesSanitizedCredentialCommands() {
        AuditService service = new AuditService();
        List<AuditEvent> observed = new ArrayList<>();
        service.addSink(observed::add);
        service.publish(AuditEvent.system(AuditSeverity.INFO, "command", "player-command", "/bigcasares:auth password sample"));
        service.publish(AuditEvent.system(AuditSeverity.INFO, "command", "player-command", "/login-help sample"));
        assertEquals("/bigcasares:auth [redacted]", observed.getFirst().message());
        assertFalse(observed.getFirst().message().contains("sample"));
        assertEquals("/login-help sample", observed.getLast().message());
    }
}
