package dev.linqfy.bigCasares.modules.discord;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

public final class DiscordFieldValidator {
    public static final Set<String> FIELDS = Set.of(
        "servidor.title", "servidor.description", "servidor.ip", "servidor.port",
        "servidor.version", "servidor.status", "servidor.color", "servidor.image",
        "servidor.thumbnail", "servidor.footer",
        "jugadores.title", "jugadores.empty-text", "jugadores.count-label", "jugadores.color",
        "jugadores.image", "jugadores.thumbnail", "jugadores.footer"
    );
    private static final Set<String> URL_FIELDS = Set.of(
        "servidor.image", "servidor.thumbnail", "jugadores.image", "jugadores.thumbnail"
    );
    private static final Set<String> COLOR_FIELDS = Set.of("servidor.color", "jugadores.color");
    private static final Map<String, Integer> LIMITS = Map.ofEntries(
        Map.entry("servidor.title", 256), Map.entry("servidor.description", 4096),
        Map.entry("servidor.ip", 1024), Map.entry("servidor.version", 1024),
        Map.entry("servidor.status", 1024), Map.entry("servidor.footer", 2048),
        Map.entry("jugadores.title", 256), Map.entry("jugadores.empty-text", 4096),
        Map.entry("jugadores.count-label", 256), Map.entry("jugadores.footer", 2048)
    );

    private DiscordFieldValidator() {
    }

    public static String validate(String field, String rawValue) {
        if (!FIELDS.contains(field)) {
            throw new IllegalArgumentException("Campo no permitido");
        }
        String value = stripMatchingQuotes(rawValue == null ? "" : rawValue.trim());
        if (COLOR_FIELDS.contains(field)) {
            if (!value.matches("#[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("El color debe usar #RRGGBB");
            }
            return value.toUpperCase(Locale.ROOT);
        }
        if ("servidor.port".equals(field)) {
            try {
                int port = Integer.parseInt(value);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException("El puerto debe estar entre 1 y 65535");
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("El puerto no es válido");
            }
        }
        if (URL_FIELDS.contains(field) && !value.isEmpty()) {
            validateUrl(value);
        }
        int limit = LIMITS.getOrDefault(field, 1024);
        if (value.length() > limit) {
            throw new IllegalArgumentException("El valor supera el límite de " + limit + " caracteres");
        }
        return value;
    }

    public static void validateConfiguration(ConfigurationSection config) {
        for (String field : FIELDS) {
            String value = config.getString("discord-integration." + field, "");
            validate(field, value);
        }
    }

    private static String stripMatchingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static void validateUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
                throw new IllegalArgumentException("La URL debe usar http o https");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("La URL no es válida");
        }
    }
}
