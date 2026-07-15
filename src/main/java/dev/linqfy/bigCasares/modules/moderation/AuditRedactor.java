package dev.linqfy.bigCasares.modules.moderation;

import java.util.regex.Pattern;

public final class AuditRedactor {
    private static final Pattern WEBHOOK = Pattern.compile("(?i)https?://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/api/webhooks/\\S+");
    private static final Pattern ASSIGNMENT = Pattern.compile("(?i)(token|secret|password|passwd|pwd|api[-_]?key)=\\S+");
    private static final Pattern PASSWORD_COMMAND = Pattern.compile("(?i)^(/(?:login|l)\\s+)\\S+");
    private static final Pattern MULTI_PASSWORD_COMMAND = Pattern.compile("(?i)^(/(?:register|changepassword|changepass|reg)\\s+).*$");

    private AuditRedactor() {
    }

    public static String redact(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String redacted = WEBHOOK.matcher(value).replaceAll("[WEBHOOK REDACTADO]");
        redacted = ASSIGNMENT.matcher(redacted).replaceAll(match -> match.group(1) + "=[REDACTADO]");
        redacted = PASSWORD_COMMAND.matcher(redacted).replaceFirst("$1[REDACTADO]");
        return MULTI_PASSWORD_COMMAND.matcher(redacted).replaceFirst("$1[REDACTADO]");
    }
}
