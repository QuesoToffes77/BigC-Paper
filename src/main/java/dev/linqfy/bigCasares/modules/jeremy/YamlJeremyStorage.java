package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

final class YamlJeremyStorage implements JeremyStorage {
    private final Path path;

    YamlJeremyStorage(Path path) {
        this.path = path;
    }

    @Override
    public Optional<JeremySnapshot> load() throws IOException {
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
        String rawPhase = yaml.getString("phase", "RESTING");
        JeremyPhase phase;
        try {
            phase = JeremyPhase.valueOf(rawPhase.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid Jeremy phase: " + rawPhase, invalid);
        }
        UUID target = uuid(yaml.getString("target-uuid"));
        UUID lastTarget = uuid(yaml.getString("last-target-uuid"));
        long phaseEndsAt = Math.max(0L, yaml.getLong("phase-ends-at-epoch-millis", 0L));
        return Optional.of(new JeremySnapshot(phase, target, lastTarget, phaseEndsAt));
    }

    @Override
    public void save(JeremySnapshot snapshot) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("phase", snapshot.phase().name());
        yaml.set("target-uuid", snapshot.targetUuid() == null ? null : snapshot.targetUuid().toString());
        yaml.set("last-target-uuid", snapshot.lastTargetUuid() == null ? null : snapshot.lastTargetUuid().toString());
        yaml.set("phase-ends-at-epoch-millis", snapshot.phaseEndsAtMillis());

        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static UUID uuid(String raw) throws IOException {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid UUID in Jeremy storage: " + raw, invalid);
        }
    }
}
