package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlControlStorage implements ControlStorage {
    private final Path path;

    public YamlControlStorage(Path path) {
        this.path = path;
    }

    @Override
    public ControlState load() {
        if (!Files.isRegularFile(path)) {
            return ControlState.defaults();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
        Optional<TimedPvpOverride> override = loadOverride(yaml);
        ResistanceLevel resistance;
        try {
            resistance = ResistanceLevel.valueOf(yaml.getString("resistance", "OFF"));
        } catch (IllegalArgumentException ex) {
            resistance = ResistanceLevel.OFF;
        }
        Map<UUID, Boolean> alerts = new HashMap<>();
        var section = yaml.getConfigurationSection("staff-alerts");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    alerts.put(UUID.fromString(key), section.getBoolean(key, true));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return new ControlState(
            yaml.getBoolean("pvp.baseline", true), override,
            yaml.getBoolean("end-access", true), yaml.getBoolean("elytra-rockets", true),
            resistance, alerts
        );
    }

    @Override
    public void save(ControlState state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pvp.baseline", state.pvpBaseline());
        state.pvpOverride().ifPresentOrElse(override -> {
            yaml.set("pvp.override.target", override.targetState());
            yaml.set("pvp.override.expires-at", override.expiresAt().toString());
            yaml.set("pvp.override.actor", override.actor());
        }, () -> yaml.set("pvp.override", null));
        yaml.set("end-access", state.endAccessEnabled());
        yaml.set("elytra-rockets", state.elytraRocketsEnabled());
        yaml.set("resistance", state.resistanceLevel().name());
        state.staffAlerts().forEach((id, enabled) -> yaml.set("staff-alerts." + id, enabled));
        try {
            Files.createDirectories(path.getParent());
            yaml.save(path.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot persist server control state", ex);
        }
    }

    private Optional<TimedPvpOverride> loadOverride(YamlConfiguration yaml) {
        if (!yaml.isSet("pvp.override.expires-at")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new TimedPvpOverride(
                yaml.getBoolean("pvp.override.target"),
                Instant.parse(yaml.getString("pvp.override.expires-at", "")),
                yaml.getString("pvp.override.actor", "Sistema")
            ));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }
}
