package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class YamlBountyStorage implements BountyStorage {

    private final Path directory;

    public YamlBountyStorage(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<BountyPlayerState> load(UUID playerId) {
        Path file = fileFor(playerId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        double activeBounty = config.getDouble("active-bounty", 0.0);
        Instant updatedAt = Instant.parse(config.getString("updated-at", Instant.EPOCH.toString()));
        return Optional.of(new BountyPlayerState(playerId, activeBounty, updatedAt));
    }

    @Override
    public void save(BountyPlayerState state) {
        try {
            Files.createDirectories(directory);

            YamlConfiguration config = new YamlConfiguration();
            config.set("active-bounty", state.activeBounty());
            config.set("updated-at", state.updatedAt().toString());
            config.save(fileFor(state.playerId()).toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la bounty de " + state.playerId(), ex);
        }
    }

    private Path fileFor(UUID playerId) {
        return directory.resolve(playerId + ".yml");
    }
}
