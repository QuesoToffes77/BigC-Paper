package dev.linqfy.bigCasares.modules.moderation;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public final class FileObservationStorage implements ObservationStorage {
    private final Path directory;
    private final Path seenPath;

    public FileObservationStorage(Path directory) {
        this.directory = directory;
        this.seenPath = directory.resolve("seen-players.yml");
    }

    @Override
    public synchronized void append(Observation observation) {
        try {
            Files.createDirectories(directory);
            LocalDate day = observation.timestamp().atZone(ZoneOffset.UTC).toLocalDate();
            String line = encode(observation) + System.lineSeparator();
            Files.writeString(
                directory.resolve(day + ".log"), line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot persist moderation observation", ex);
        }
    }

    @Override
    public synchronized boolean hasSeen(UUID playerId) {
        if (!Files.isRegularFile(seenPath)) {
            return false;
        }
        return YamlConfiguration.loadConfiguration(seenPath.toFile()).isSet(playerId.toString());
    }

    @Override
    public synchronized void markSeen(UUID playerId) {
        YamlConfiguration yaml = Files.isRegularFile(seenPath)
            ? YamlConfiguration.loadConfiguration(seenPath.toFile())
            : new YamlConfiguration();
        yaml.set(playerId.toString(), Instant.now().toString());
        try {
            Files.createDirectories(directory);
            yaml.save(seenPath.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot persist first-seen state", ex);
        }
    }

    @Override
    public synchronized void purgeOlderThan(Instant cutoff) {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (var paths = Files.list(directory)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".log"))
                .filter(path -> isOlder(path, cutoff))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                });
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot purge moderation observations", ex);
        }
    }

    private boolean isOlder(Path path, Instant cutoff) {
        String name = path.getFileName().toString().replace(".log", "");
        try {
            return LocalDate.parse(name).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().isBefore(cutoff);
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private String encode(Observation observation) {
        StringBuilder line = new StringBuilder();
        line.append(observation.timestamp()).append('\t')
            .append(observation.playerId()).append('\t')
            .append(sanitize(observation.type()));
        observation.values().forEach((key, value) -> line.append('\t')
            .append(sanitize(key)).append('=').append(sanitize(value)));
        return line.toString();
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
