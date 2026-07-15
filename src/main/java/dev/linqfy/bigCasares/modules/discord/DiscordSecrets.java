package dev.linqfy.bigCasares.modules.discord;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public record DiscordSecrets(String token, String bridgeWebhookUrl) {

    public DiscordSecrets {
        token = token == null ? "" : token.trim();
        bridgeWebhookUrl = bridgeWebhookUrl == null ? "" : bridgeWebhookUrl.trim();
    }

    public static DiscordSecrets loadOrCreate(Path path) {
        if (!Files.exists(path)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("token", "");
            yaml.set("bridge-webhook-url", "");
            try {
                Files.createDirectories(path.getParent());
                yaml.save(path.toFile());
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot create discord-secrets.yml", ex);
            }
        }
        applyOwnerOnlyPermissions(path);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
        return new DiscordSecrets(
            yaml.getString("token", ""),
            yaml.getString("bridge-webhook-url", "")
        );
    }

    private static void applyOwnerOnlyPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
        }
    }
}
