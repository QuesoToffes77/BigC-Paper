package dev.linqfy.bigCasares.communication;

import java.util.Locale;
import java.util.Set;

/** Recognizes Auth command labels before any audit processing sees their arguments. */
public final class CredentialCommandSanitizer {
    private static final Set<String> LABELS = Set.of("login", "l", "log", "register", "reg", "auth", "cuenta");

    private CredentialCommandSanitizer() {
    }

    public static boolean isCredentialCommand(String command) {
        return labelEnd(command) >= 0;
    }

    public static String redact(String command) {
        int end = labelEnd(command);
        if (end < 0 || end == command.length()) {
            return command;
        }
        return command.substring(0, end) + " [redacted]";
    }

    private static int labelEnd(String command) {
        if (command == null) {
            return -1;
        }
        int start = 0;
        while (start < command.length() && Character.isWhitespace(command.charAt(start))) {
            start++;
        }
        if (start < command.length() && command.charAt(start) == '/') {
            start++;
        }
        int end = start;
        while (end < command.length() && !Character.isWhitespace(command.charAt(end))) {
            end++;
        }
        String label = command.substring(start, end).toLowerCase(Locale.ROOT);
        int separator = label.lastIndexOf(':');
        return LABELS.contains(separator < 0 ? label : label.substring(separator + 1)) ? end : -1;
    }
}
