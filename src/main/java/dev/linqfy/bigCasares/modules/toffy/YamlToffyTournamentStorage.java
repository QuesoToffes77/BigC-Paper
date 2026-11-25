package dev.linqfy.bigCasares.modules.toffy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every tournament in one file, {@code torneos.yml}. Written atomically after each change. A tournament saved by an
 * older version with one {@code place} section loads as a one-point list of places.
 */
final class YamlToffyTournamentStorage implements ToffyTournamentStorage {
    private final Path file;

    YamlToffyTournamentStorage(Path file) {
        this.file = file;
    }

    /**
     * Loads the tournaments. A file that cannot be read is renamed to {@code <file>.broken-<millis>} before the
     * error is thrown, so the next save does not overwrite it.
     */
    @Override
    public List<ToffyTournament> load() throws IOException {
        if (!Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            return parse();
        } catch (IOException broken) {
            Path kept = quarantine();
            throw new IOException(broken.getMessage() + " (kept as " + kept.getFileName() + ")", broken);
        }
    }

    private Path quarantine() throws IOException {
        String base = file.getFileName() + ".broken-" + System.currentTimeMillis();
        Path target = file.resolveSibling(base);
        for (int attempt = 1; Files.exists(target); attempt++) {
            target = file.resolveSibling(base + "-" + attempt);
        }
        Files.move(file, target);
        return target;
    }

    private List<ToffyTournament> parse() throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        } catch (InvalidConfigurationException invalid) {
            throw new IOException("Invalid tournaments file " + file + ": " + invalid.getMessage(), invalid);
        }
        List<ToffyTournament> result = new ArrayList<>();
        ConfigurationSection root = yaml.getConfigurationSection("tournaments");
        if (root == null) {
            return result;
        }
        try {
            for (String key : root.getKeys(false)) {
                ConfigurationSection section = root.getConfigurationSection(key);
                if (section == null) {
                    throw new IOException("Tournament " + key + " is not a section in " + file);
                }
                result.add(read(key, section));
            }
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid tournaments file " + file + ": " + invalid.getMessage(), invalid);
        }
        return result;
    }

    static ToffyTournament read(String key, ConfigurationSection section) {
        ToffyTournament tournament = new ToffyTournament(ToffyTournament.keyOf(key), section.getString("name", key));
        tournament.setOpen(section.getBoolean("open", true));
        ConfigurationSection participants = section.getConfigurationSection("participants");
        if (participants != null) {
            for (String id : participants.getKeys(false)) {
                tournament.putParticipant(UUID.fromString(id), participants.getString(id, ""));
            }
        }
        ConfigurationSection messages = section.getConfigurationSection("messages");
        if (messages != null) {
            for (String caseKey : messages.getKeys(false)) {
                tournament.putMessage(caseKey, messages.getString(caseKey, ""));
            }
        }
        ConfigurationSection returns = section.getConfigurationSection("returns");
        if (returns != null) {
            for (String id : returns.getKeys(false)) {
                ConfigurationSection at = returns.getConfigurationSection(id);
                if (at == null) {
                    throw new IllegalArgumentException("Saved location of " + id + " is not a section");
                }
                tournament.putReturn(UUID.fromString(id), readLocation(at));
            }
        }
        for (String id : section.getStringList("pending")) {
            tournament.addPending(UUID.fromString(id));
        }
        readPlaces(section, tournament);
        if (section.contains("scheduled")) {
            tournament.setScheduledAt(section.getLong("scheduled"));
        }
        if (section.contains("nivel.min") || section.contains("nivel.max")) {
            tournament.setLevelRange(
                section.contains("nivel.min") ? section.getDouble("nivel.min") : null,
                section.contains("nivel.max") ? section.getDouble("nivel.max") : null);
        }
        if (section.contains("cupo")) {
            tournament.setCapacity(section.getInt("cupo"));
        }
        if (section.contains("minimo")) {
            tournament.setMinPlayers(section.getInt("minimo"));
        }
        if (section.contains("cierre")) {
            tournament.setCloseAt(section.getLong("cierre"));
        }
        if (section.contains("evento.at")) {
            tournament.setEvent(section.getString("evento.tipo", ""),
                section.contains("evento.segundos") ? section.getInt("evento.segundos") : null,
                section.getLong("evento.at"));
        }
        return tournament;
    }

    /** {@code places} is a list; the old single {@code place} section becomes a one-point list. */
    private static void readPlaces(ConfigurationSection section, ToffyTournament tournament) {
        if (section.contains("places")) {
            for (Map<?, ?> point : section.getMapList("places")) {
                tournament.addPlace(readPoint(point));
            }
            return;
        }
        ConfigurationSection legacy = section.getConfigurationSection("place");
        if (legacy != null) {
            tournament.addPlace(readLocation(legacy));
        }
    }

    private static ToffyStoredLocation readPoint(Map<?, ?> point) {
        return new ToffyStoredLocation(String.valueOf(point.get("world")),
            number(point, "x"), number(point, "y"), number(point, "z"),
            (float) number(point, "yaw"), (float) number(point, "pitch"));
    }

    private static double number(Map<?, ?> point, String key) {
        Object value = point.get(key);
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Place value " + key + " is not a number");
        }
        return number.doubleValue();
    }

    private static ToffyStoredLocation readLocation(ConfigurationSection at) {
        return new ToffyStoredLocation(at.getString("world", ""), at.getDouble("x"), at.getDouble("y"),
            at.getDouble("z"), (float) at.getDouble("yaw"), (float) at.getDouble("pitch"));
    }

    private static void writeLocation(YamlConfiguration yaml, String path, ToffyStoredLocation at) {
        yaml.set(path + ".world", at.world());
        yaml.set(path + ".x", at.x());
        yaml.set(path + ".y", at.y());
        yaml.set(path + ".z", at.z());
        yaml.set(path + ".yaw", (double) at.yaw());
        yaml.set(path + ".pitch", (double) at.pitch());
    }

    @Override
    public void save(Collection<ToffyTournament> tournaments) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ToffyTournament tournament : tournaments) {
            String base = "tournaments." + tournament.key();
            yaml.set(base + ".name", tournament.displayName());
            yaml.set(base + ".open", tournament.open());
            tournament.participants().forEach((id, name) -> yaml.set(base + ".participants." + id, name));
            tournament.messages().forEach((caseKey, text) -> yaml.set(base + ".messages." + caseKey, text));
            tournament.returns().forEach((id, at) -> writeLocation(yaml, base + ".returns." + id, at));
            yaml.set(base + ".pending", tournament.pending().stream().map(UUID::toString).toList());
            if (!tournament.places().isEmpty()) {
                yaml.set(base + ".places", tournament.places().stream().map(ToffyTournamentStorage::pointOf).toList());
            }
            yaml.set(base + ".scheduled", tournament.scheduledAt());
            yaml.set(base + ".nivel.min", tournament.minLevel());
            yaml.set(base + ".nivel.max", tournament.maxLevel());
            yaml.set(base + ".cupo", tournament.capacity());
            yaml.set(base + ".minimo", tournament.minPlayers());
            yaml.set(base + ".cierre", tournament.closeAt());
            yaml.set(base + ".evento.tipo", tournament.eventType());
            yaml.set(base + ".evento.segundos", tournament.eventLobby());
            yaml.set(base + ".evento.at", tournament.eventAt());
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
