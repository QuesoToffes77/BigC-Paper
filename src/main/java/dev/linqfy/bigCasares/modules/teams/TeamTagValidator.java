package dev.linqfy.bigCasares.modules.teams;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;

public final class TeamTagValidator {

    public static final int MIN_LENGTH = 1;
    public static final int MAX_LENGTH = 6;
    private static volatile Set<String> allowedSymbols = Set.of("⚔", "★", "❤", "☠", "◆", "✦", "🔥", "🛡");

    public static void configureAllowedSymbols(Collection<String> symbols) {
        LinkedHashSet<String> safe = new LinkedHashSet<>();
        for (String symbol : symbols) {
            if (symbol != null && !symbol.isBlank() && symbol.indexOf('§') < 0 && symbol.indexOf('&') < 0) safe.add(symbol);
        }
        allowedSymbols = Set.copyOf(safe);
    }

    public TeamTagValidator() {
    }

    public static boolean isValid(String tag) {
        if (tag == null || !tag.equals(tag.strip()) || tag.indexOf('§') >= 0 || tag.indexOf('&') >= 0) return false;
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(tag);
        int visible = 0;
        for (int start = iterator.first(), end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            visible++;
            int codePoint = tag.codePointAt(start);
            String grapheme = tag.substring(start, end);
            boolean explicitlyAllowed = allowedSymbols.contains(grapheme);
            if (grapheme.codePoints().anyMatch(value -> Character.isISOControl(value)
                    || Character.getType(value) == Character.FORMAT
                    || Character.getType(value) == Character.SURROGATE
                    || Character.isWhitespace(value)
                    || (Character.getType(value) == Character.PRIVATE_USE && !explicitlyAllowed))) return false;
            if (!Character.isLetterOrDigit(codePoint) && !explicitlyAllowed) return false;
        }
        return visible >= MIN_LENGTH && visible <= MAX_LENGTH;
    }

    public static String validate(String tag) {
        if (!isValid(tag)) {
            throw new IllegalArgumentException("El tag debe contener entre " + MIN_LENGTH + " y " + MAX_LENGTH + " símbolos Unicode visibles y seguros.");
        }
        return tag;
    }

    public static String normalizeAndValidate(String tag) {
        if (tag == null) {
            return validate(null);
        }
        return validate(tag.trim().toUpperCase(Locale.ROOT));
    }
}
