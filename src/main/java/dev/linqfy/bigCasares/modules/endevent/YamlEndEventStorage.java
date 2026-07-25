package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class YamlEndEventStorage implements EndEventStorage {
    private final Path path;

    public YamlEndEventStorage(Path path) {
        this.path = path;
    }

    @Override
    public synchronized Optional<EndEventSnapshot> load() {
        YamlConfiguration yaml = loadYaml();
        if (!yaml.isSet("domain.phase")) {
            return Optional.empty();
        }
        EndEventPhase phase = parseEnum(
            EndEventPhase.class, yaml.getString("domain.phase"), EndEventPhase.ARMED
        );
        Optional<Instant> huntStartedAt = parseInstant(yaml.getString("domain.hunt-started-at"));
        Optional<UUID> eggCarrier = parseUuid(yaml.getString("domain.egg-carrier"));
        Map<UUID, EndEventParticipantSnapshot> participants = new LinkedHashMap<>();
        ConfigurationSection participantSection = yaml.getConfigurationSection("domain.participants");
        if (participantSection != null) {
            for (String rawId : participantSection.getKeys(false)) {
                parseUuid(rawId).ifPresent(id -> {
                    String base = "domain.participants." + rawId + ".";
                    participants.put(id, new EndEventParticipantSnapshot(
                        yaml.getBoolean(base + "eliminated"),
                        yaml.getBoolean(base + "online"),
                        yaml.getInt(base + "disconnects"),
                        parseInstant(yaml.getString(base + "combat-until"))
                    ));
                });
            }
        }
        Set<EndEventMilestone> milestones = new LinkedHashSet<>();
        for (String raw : yaml.getStringList("domain.claimed-milestones")) {
            EndEventMilestone parsed = parseEnum(EndEventMilestone.class, raw, null);
            if (parsed != null) {
                milestones.add(parsed);
            }
        }
        return Optional.of(new EndEventSnapshot(
            phase, huntStartedAt, eggCarrier, participants, milestones
        ));
    }

    @Override
    public synchronized void save(EndEventSnapshot snapshot) {
        YamlConfiguration yaml = loadYaml();
        yaml.set("domain", null);
        yaml.set("domain.phase", snapshot.phase().name());
        yaml.set("domain.hunt-started-at", snapshot.huntStartedAt().map(Instant::toString).orElse(null));
        yaml.set("domain.egg-carrier", snapshot.eggCarrier().map(UUID::toString).orElse(null));
        snapshot.participants().forEach((id, state) -> {
            String base = "domain.participants." + id + ".";
            yaml.set(base + "eliminated", state.eliminated());
            yaml.set(base + "online", state.online());
            yaml.set(base + "disconnects", state.disconnects());
            yaml.set(base + "combat-until", state.combatUntil().map(Instant::toString).orElse(null));
        });
        yaml.set("domain.claimed-milestones",
            snapshot.claimedMilestones().stream().map(Enum::name).sorted().toList());
        saveYaml(yaml);
    }

    public synchronized EndEventRuntimeState loadRuntime() {
        YamlConfiguration yaml = loadYaml();
        Set<UUID> optedIn = parseUuids(yaml.getStringList("runtime.opted-in-operators"));
        Map<UUID, String> gameModes = new LinkedHashMap<>();
        ConfigurationSection gameModeSection = yaml.getConfigurationSection("runtime.original-game-modes");
        if (gameModeSection != null) {
            for (String rawId : gameModeSection.getKeys(false)) {
                parseUuid(rawId).ifPresent(id ->
                    gameModes.put(id, gameModeSection.getString(rawId, "SURVIVAL")));
            }
        }
        Map<UUID, List<ItemStack>> escrow = new LinkedHashMap<>();
        ConfigurationSection escrowSection = yaml.getConfigurationSection("runtime.escrow");
        if (escrowSection != null) {
            for (String rawId : escrowSection.getKeys(false)) {
                parseUuid(rawId).ifPresent(id -> {
                    List<ItemStack> items = new ArrayList<>();
                    for (Object value : escrowSection.getList(rawId, List.of())) {
                        if (value instanceof ItemStack item) {
                            items.add(item);
                        }
                    }
                    escrow.put(id, List.copyOf(items));
                });
            }
        }
        return new EndEventRuntimeState(
            yaml.getBoolean("runtime.delayed-reveal"),
            readLocation(yaml, "runtime.portal"),
            readLocation(yaml, "runtime.egg-block"),
            optedIn,
            readBorder(yaml, "runtime.original-border"),
            gameModes,
            escrow,
            parseUuid(yaml.getString("runtime.winner")),
            parseInstant(yaml.getString("runtime.ban-at"))
        );
    }

    public synchronized void saveRuntime(EndEventRuntimeState state) {
        YamlConfiguration yaml = loadYaml();
        yaml.set("runtime", null);
        yaml.set("runtime.delayed-reveal", state.delayedReveal());
        writeLocation(yaml, "runtime.portal", state.portal());
        writeLocation(yaml, "runtime.egg-block", state.eggBlock());
        yaml.set("runtime.opted-in-operators",
            state.optedInOperators().stream().map(UUID::toString).sorted().toList());
        writeBorder(yaml, "runtime.original-border", state.originalBorder());
        state.originalGameModes().forEach((id, mode) ->
            yaml.set("runtime.original-game-modes." + id, mode));
        state.escrow().forEach((id, items) ->
            yaml.set("runtime.escrow." + id, items));
        yaml.set("runtime.winner", state.winner().map(UUID::toString).orElse(null));
        yaml.set("runtime.ban-at", state.banAt().map(Instant::toString).orElse(null));
        saveYaml(yaml);
    }

    private Optional<EndEventLocation> readLocation(YamlConfiguration yaml, String base) {
        String world = yaml.getString(base + ".world");
        if (world == null || world.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new EndEventLocation(
            world,
            yaml.getDouble(base + ".x"),
            yaml.getDouble(base + ".y"),
            yaml.getDouble(base + ".z")
        ));
    }

    private void writeLocation(
        YamlConfiguration yaml,
        String base,
        Optional<EndEventLocation> location
    ) {
        location.ifPresent(value -> {
            yaml.set(base + ".world", value.world());
            yaml.set(base + ".x", value.x());
            yaml.set(base + ".y", value.y());
            yaml.set(base + ".z", value.z());
        });
    }

    private Optional<EndEventBorderSnapshot> readBorder(YamlConfiguration yaml, String base) {
        String world = yaml.getString(base + ".world");
        if (world == null || world.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new EndEventBorderSnapshot(
            world,
            yaml.getDouble(base + ".center-x"),
            yaml.getDouble(base + ".center-z"),
            yaml.getDouble(base + ".size")
        ));
    }

    private void writeBorder(
        YamlConfiguration yaml,
        String base,
        Optional<EndEventBorderSnapshot> border
    ) {
        border.ifPresent(value -> {
            yaml.set(base + ".world", value.world());
            yaml.set(base + ".center-x", value.centerX());
            yaml.set(base + ".center-z", value.centerZ());
            yaml.set(base + ".size", value.size());
        });
    }

    private YamlConfiguration loadYaml() {
        return Files.exists(path) ? YamlConfiguration.loadConfiguration(path.toFile()) : new YamlConfiguration();
    }

    private void saveYaml(YamlConfiguration yaml) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not persist end event state at " + path, failure);
        }
    }

    private Set<UUID> parseUuids(List<String> values) {
        Set<UUID> result = new LinkedHashSet<>();
        values.forEach(value -> parseUuid(value).ifPresent(result::add));
        return Set.copyOf(result);
    }

    private Optional<UUID> parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Instant> parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(raw));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String raw, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
