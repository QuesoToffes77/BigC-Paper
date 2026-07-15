package dev.linqfy.bigCasares.modules.discord;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YamlDiscordStateStorage {
    private final Path path;

    public YamlDiscordStateStorage(Path path) {
        this.path = path;
    }

    public synchronized DiscordMessageState load() {
        if (!Files.isRegularFile(path)) {
            return new DiscordMessageState(0, 0);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
        return new DiscordMessageState(yaml.getLong("server-message-id"), yaml.getLong("player-message-id"));
    }

    public synchronized void save(DiscordMessageState state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("server-message-id", state.serverMessageId());
        yaml.set("player-message-id", state.playerMessageId());
        try {
            Files.createDirectories(path.getParent());
            yaml.save(path.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot persist Discord managed message IDs", ex);
        }
    }
}
