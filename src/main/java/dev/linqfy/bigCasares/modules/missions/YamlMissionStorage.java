package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlMissionStorage implements MissionStorage {

    private final Path rootDirectory;

    public YamlMissionStorage(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    @Override
    public Optional<MissionPlayerState> load(UUID playerId) {
        Path file = resolveFile(playerId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        return Optional.of(new MissionPlayerState(
            playerId,
            Instant.parse(yaml.getString("generated-at")),
            Instant.parse(yaml.getString("daily-resets-at")),
            Instant.parse(yaml.getString("weekly-resets-at")),
            readAssignments(yaml.getConfigurationSection("daily")),
            readAssignments(yaml.getConfigurationSection("weekly"))
        ));
    }

    @Override
    public void save(MissionPlayerState state) {
        try {
            Files.createDirectories(rootDirectory);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("player-id", state.playerId().toString());
            yaml.set("generated-at", state.generatedAt().toString());
            yaml.set("daily-resets-at", state.dailyResetsAt().toString());
            yaml.set("weekly-resets-at", state.weeklyResetsAt().toString());
            writeAssignments(yaml.createSection("daily"), state.dailyAssignments());
            writeAssignments(yaml.createSection("weekly"), state.weeklyAssignments());
            yaml.save(resolveFile(state.playerId()).toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save mission state for " + state.playerId(), ex);
        }
    }

    private Map<String, MissionAssignment> readAssignments(ConfigurationSection section) {
        Map<String, MissionAssignment> assignments = new LinkedHashMap<>();
        if (section == null) {
            return assignments;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }

            ConfigurationSection paramsSection = entry.getConfigurationSection("params");
            Map<String, Object> params = paramsSection == null ? Map.of() : paramsSection.getValues(false);
            MissionDefinition definition = new MissionDefinition(
                entry.getString("definition-id", id),
                MissionScope.valueOf(entry.getString("scope", "DAILY")),
                entry.getString("title", id),
                entry.getString("description", ""),
                MissionType.valueOf(entry.getString("type", "HOLD_EXACT_ITEM_COUNT")),
                entry.getInt("goal"),
                entry.getDouble("reward"),
                entry.getInt("weight", 1),
                entry.getBoolean("enabled", true),
                params
            );
            MissionProgressSnapshot snapshot = new MissionProgressSnapshot(
                entry.getInt("progress"),
                entry.getBoolean("completed"),
                entry.getBoolean("claimed")
            );
            assignments.put(id, new MissionAssignment(definition, snapshot));
        }

        return assignments;
    }

    private void writeAssignments(ConfigurationSection root, Map<String, MissionAssignment> assignments) {
        for (Map.Entry<String, MissionAssignment> entry : assignments.entrySet()) {
            ConfigurationSection section = root.createSection(entry.getKey());
            MissionAssignment assignment = entry.getValue();
            MissionDefinition definition = assignment.definition();
            MissionProgressSnapshot snapshot = assignment.snapshot();

            section.set("definition-id", definition.id());
            section.set("scope", definition.scope().name());
            section.set("title", definition.title());
            section.set("description", definition.description());
            section.set("type", definition.type().name());
            section.set("goal", definition.goal());
            section.set("reward", definition.reward());
            section.set("weight", definition.weight());
            section.set("enabled", definition.enabled());
            section.set("params", definition.params());
            section.set("progress", snapshot.progress());
            section.set("completed", snapshot.completed());
            section.set("claimed", snapshot.claimed());
        }
    }

    private Path resolveFile(UUID playerId) {
        return rootDirectory.resolve(playerId + ".yml");
    }
}
