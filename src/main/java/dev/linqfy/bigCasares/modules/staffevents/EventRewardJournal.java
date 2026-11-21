package dev.linqfy.bigCasares.modules.staffevents;

import dev.linqfy.bigCasares.module.runtime.PlayerDataCheckpoint;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable evidence for a reward that an operator must reconcile manually. */
final class EventRewardJournal {
    record Pending(UUID eventId, UUID playerId, String playerName, StaffEventType type,
                   Instant time, StaffEventSettings.Rewards rewards) {
        Pending {
            Objects.requireNonNull(eventId, "eventId");
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(playerName, "playerName");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(time, "time");
            Objects.requireNonNull(rewards, "rewards");
        }
    }

    private final Path directory;

    EventRewardJournal(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    boolean hasPending() throws IOException {
        if (Files.notExists(directory)) return false;
        try (var files = Files.newDirectoryStream(directory, "*.yml")) {
            return files.iterator().hasNext();
        }
    }

    Path directory() {
        return directory;
    }

    Path record(Pending pending) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("status", "manual-review");
        yaml.set("reason", "inventory-restoration-unconfirmed");
        yaml.set("event-id", pending.eventId().toString());
        yaml.set("player-id", pending.playerId().toString());
        yaml.set("player-name", pending.playerName());
        yaml.set("event-type", pending.type().name());
        yaml.set("recorded-at", pending.time().toString());
        yaml.set("rewards.items", pending.rewards().items());
        yaml.set("rewards.commands", pending.rewards().commands());
        String content = yaml.saveToString();
        Files.createDirectories(directory);
        Path target = directory.resolve(pending.eventId() + "-" + pending.playerId() + ".yml");
        if (Files.exists(target)) {
            if (!Files.readString(target, StandardCharsets.UTF_8).equals(content)) {
                throw new IOException("Conflicting pending reward evidence: " + target);
            }
        } else {
            Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            PlayerDataCheckpoint.forceFile(temporary);
            try {
                // A hard link publishes the fully written file without ever replacing existing evidence.
                Files.createLink(target, temporary);
            } catch (FileAlreadyExistsException conflict) {
                if (!Files.readString(target, StandardCharsets.UTF_8).equals(content)) {
                    throw new IOException("Conflicting pending reward evidence: " + target, conflict);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
        PlayerDataCheckpoint.forceFile(target);
        PlayerDataCheckpoint.forceDirectory(directory);
        PlayerDataCheckpoint.forceDirectory(directory.getParent());
        return target;
    }
}
