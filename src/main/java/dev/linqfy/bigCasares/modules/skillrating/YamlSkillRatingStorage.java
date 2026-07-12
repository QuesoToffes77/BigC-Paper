package dev.linqfy.bigCasares.modules.skillrating;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class YamlSkillRatingStorage implements SkillRatingStorage {

    private final Path directory;

    public YamlSkillRatingStorage(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<SkillRatingState> load(UUID playerId) {
        Path file = fileFor(playerId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        double mu = config.getDouble("mu", 25.0);
        double sigma = config.getDouble("sigma", 25.0 / 3.0);
        double skillRating = config.getDouble("skill-rating", mu - 3.0 * sigma);
        int tier = config.getInt("tier", 1);
        Instant updatedAt = Instant.parse(config.getString("updated-at", Instant.EPOCH.toString()));
        return Optional.of(new SkillRatingState(playerId, mu, sigma, skillRating, tier, updatedAt));
    }

    @Override
    public void save(SkillRatingState state) {
        try {
            Files.createDirectories(directory);

            YamlConfiguration config = new YamlConfiguration();
            config.set("mu", state.mu());
            config.set("sigma", state.sigma());
            config.set("skill-rating", state.skillRating());
            config.set("tier", state.tier());
            config.set("updated-at", state.updatedAt().toString());
            config.save(fileFor(state.playerId()).toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el skill rating de " + state.playerId(), ex);
        }
    }

    private Path fileFor(UUID playerId) {
        return directory.resolve(playerId + ".yml");
    }

    @Override
    public java.util.List<SkillRatingState> loadAll() {
        if (!Files.exists(directory)) return java.util.List.of();
        try (java.util.stream.Stream<Path> stream = Files.list(directory)) {
            return stream.filter(p -> p.toString().endsWith(".yml"))
                .map(p -> {
                    String name = p.getFileName().toString();
                    try {
                        return load(UUID.fromString(name.substring(0, name.length() - 4))).orElse(null);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (IOException e) {
            return java.util.List.of();
        }
    }
}
