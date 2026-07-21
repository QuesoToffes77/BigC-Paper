package dev.linqfy.bigCasares.modules.danger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlDangerStorage implements DangerStorage {
    private final Path directory;

    public YamlDangerStorage(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<DangerState> load(UUID playerId) {
        Path file = fileFor(playerId);
        if (!Files.exists(file)) return Optional.empty();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        Map<UUID, List<Instant>> history = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("repeated-kills");
        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                try {
                    UUID victimId = UUID.fromString(rawId);
                    List<Instant> entries = section.getLongList(rawId).stream().map(Instant::ofEpochMilli).toList();
                    history.put(victimId, entries);
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed historical entries while retaining the player state.
                }
            }
        }
        return Optional.of(new DangerState(
            playerId,
            yaml.getDouble("activity-score", 0.0),
            Instant.parse(yaml.getString("last-active-at", Instant.EPOCH.toString())),
            yaml.getInt("departure-tier", 1),
            history
        ));
    }

    @Override
    public void save(DangerState state) {
        try {
            Files.createDirectories(directory);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("activity-score", state.activityScore());
            yaml.set("last-active-at", state.lastActiveAt().toString());
            yaml.set("departure-tier", state.departureTier());
            state.repeatedKills().forEach((victim, entries) -> yaml.set(
                "repeated-kills." + victim, entries.stream().map(Instant::toEpochMilli).toList()
            ));
            yaml.save(fileFor(state.playerId()).toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save danger state for " + state.playerId(), exception);
        }
    }

    @Override
    public List<DangerState> loadAll() {
        if (!Files.isDirectory(directory)) return List.of();
        try (var files = Files.list(directory)) {
            List<DangerState> states = new ArrayList<>();
            files.filter(path -> path.getFileName().toString().endsWith(".yml")).forEach(path -> {
                String name = path.getFileName().toString();
                try {
                    load(UUID.fromString(name.substring(0, name.length() - 4))).ifPresent(states::add);
                } catch (IllegalArgumentException ignored) {
                    // Ignore unrelated YAML files in the player directory.
                }
            });
            return List.copyOf(states);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load danger standings", exception);
        }
    }

    private Path fileFor(UUID playerId) {
        return directory.resolve(playerId + ".yml");
    }
}
