package dev.linqfy.bigCasares.modules.discord;

import dev.linqfy.bigCasares.modules.moderation.AuditEvent;
import dev.linqfy.bigCasares.modules.moderation.AuditSeverity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.configuration.ConfigurationSection;

import java.awt.Color;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DiscordEmbedFactory {
    private final ConfigurationSection config;

    public DiscordEmbedFactory(ConfigurationSection config) {
        this.config = config;
    }

    public MessageEmbed server(boolean online) {
        String root = "discord-integration.servidor.";
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(value(root + "title", "BigCasares"))
            .setDescription(value(root + "description", "Información oficial del servidor"))
            .setColor(color(value(root + "color", "#2ECC71")))
            .addField("IP", value(root + "ip", "play.example.com"), true)
            .addField("Puerto", value(root + "port", "25565"), true)
            .addField("Versión", value(root + "version", "Paper 26.2"), true)
            .addField("Estado", online ? value(root + "status", "En línea") : "Fuera de línea", false);
        optionalMedia(builder, value(root + "image", ""), value(root + "thumbnail", ""));
        String footer = value(root + "footer", "BigCasares");
        if (!footer.isBlank()) {
            builder.setFooter(footer);
        }
        return builder.build();
    }

    public MessageEmbed players(List<String> playerNames) {
        String root = "discord-integration.jugadores.";
        List<String> sorted = playerNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(value(root + "title", "Jugadores activos"))
            .setColor(color(value(root + "color", "#3498DB")));
        String description = sorted.isEmpty()
            ? value(root + "empty-text", "No hay jugadores conectados.")
            : truncatePlayers(sorted);
        builder.setDescription(description);
        builder.addField(value(root + "count-label", "Cantidad"), String.valueOf(sorted.size()), true);
        optionalMedia(builder, value(root + "image", ""), value(root + "thumbnail", ""));
        String footer = value(root + "footer", "Lista de jugadores visibles");
        if (!footer.isBlank()) {
            builder.setFooter(footer);
        }
        return builder.build();
    }

    public MessageEmbed audit(AuditEvent event, int duplicates) {
        EmbedBuilder builder = new EmbedBuilder()
            .setTitle(title(event.severity()))
            .setColor(severityColor(event.severity()))
            .setDescription(limit(event.message(), 4096))
            .setTimestamp(event.timestamp());
        builder.addField("Categoría", limit(event.category(), 1024), true);
        builder.addField("Acción", limit(event.action(), 1024), true);
        if (!event.playerName().isBlank()) {
            builder.addField("Jugador", limit(event.playerName(), 1024), true);
        }
        int count = 0;
        for (Map.Entry<String, String> detail : event.details().entrySet()) {
            if (count++ >= 20) {
                break;
            }
            builder.addField(limit(detail.getKey(), 256), limit(detail.getValue(), 1024), false);
        }
        if (duplicates > 1) {
            builder.setFooter("Evento repetido " + duplicates + " veces en 5 segundos");
        }
        return builder.build();
    }

    private String truncatePlayers(List<String> players) {
        List<String> included = new ArrayList<>();
        int length = 0;
        for (String player : players) {
            String line = "• " + player;
            int separator = included.isEmpty() ? 0 : 1;
            int omitted = players.size() - included.size() - 1;
            String suffix = omitted > 0 ? "\n… y " + omitted + " más" : "";
            if (length + separator + line.length() + suffix.length() > 4096) {
                break;
            }
            included.add(line);
            length += separator + line.length();
        }
        int omitted = players.size() - included.size();
        String result = String.join("\n", included);
        if (omitted > 0) {
            String suffix = "… y " + omitted + " más";
            while (!included.isEmpty() && result.length() + 1 + suffix.length() > 4096) {
                included.removeLast();
                omitted++;
                suffix = "… y " + omitted + " más";
                result = String.join("\n", included);
            }
            result += (result.isEmpty() ? "" : "\n") + suffix;
        }
        return result;
    }

    private void optionalMedia(EmbedBuilder builder, String image, String thumbnail) {
        if (!image.isBlank()) {
            builder.setImage(image);
        }
        if (!thumbnail.isBlank()) {
            builder.setThumbnail(thumbnail);
        }
    }

    private Color color(String value) {
        try {
            return Color.decode(value);
        } catch (NumberFormatException ex) {
            return new Color(46, 204, 113);
        }
    }

    private Color severityColor(AuditSeverity severity) {
        return switch (severity) {
            case INFO -> new Color(52, 152, 219);
            case WARNING -> new Color(241, 196, 15);
            case SEVERE -> new Color(230, 126, 34);
            case CRITICAL -> new Color(231, 76, 60);
        };
    }

    private String title(AuditSeverity severity) {
        return switch (severity) {
            case INFO -> "Información del servidor";
            case WARNING -> "Advertencia del servidor";
            case SEVERE -> "Error grave del servidor";
            case CRITICAL -> "Alerta crítica de moderación";
        };
    }

    private String value(String path, String fallback) {
        return config.getString(path, fallback);
    }

    private String limit(String value, int limit) {
        String safe = value == null || value.isBlank() ? "Sin detalles" : value;
        return safe.length() <= limit ? safe : safe.substring(0, limit - 1) + "…";
    }
}
