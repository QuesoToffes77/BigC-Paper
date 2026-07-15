package dev.linqfy.bigCasares.communication;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmojiAliasService {
    private static final Pattern TOKEN = Pattern.compile("(?<![\\p{L}\\p{N}_:])(:[\\p{L}\\p{N}_+\\-]+:)(?![\\p{L}\\p{N}_:])");

    private final Map<String, String> aliases;

    public EmojiAliasService(Map<String, String> aliases) {
        Objects.requireNonNull(aliases, "aliases");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        aliases.forEach((alias, value) -> {
            if (alias != null && alias.matches(":[\\p{L}\\p{N}_+\\-]+:") && value != null) {
                copy.put(alias, value);
            }
        });
        this.aliases = Map.copyOf(copy);
    }

    public static EmojiAliasService defaults() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(":skull:", "☠");
        values.put(":heart:", "❤");
        values.put(":star:", "★");
        values.put(":swords:", "⚔");
        values.put(":fire:", "🔥");
        values.put(":shield:", "🛡");
        return new EmojiAliasService(values);
    }

    public String replace(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = TOKEN.matcher(input);
        StringBuilder output = new StringBuilder(input.length());
        while (matcher.find()) {
            String replacement = aliases.getOrDefault(matcher.group(1), matcher.group(1));
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public Map<String, String> aliases() {
        return aliases;
    }
}
