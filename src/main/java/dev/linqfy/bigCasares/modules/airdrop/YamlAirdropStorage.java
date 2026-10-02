package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class YamlAirdropStorage implements AirdropStorage {

    private final Path dataFolder;
    private final Logger logger;

    public YamlAirdropStorage(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    private Path file() {
        return dataFolder.resolve("airdrop.yml");
    }

    @Override
    public void save(AirdropData data) {
        YamlConfiguration yaml = readAll();
        yaml.set("position.x", data.position().x());
        yaml.set("position.y", data.position().y());
        yaml.set("position.z", data.position().z());
        yaml.set("id", data.id().toString());
        yaml.set("type", data.type().name());
        yaml.set("quality", data.quality().name());
        yaml.set("phase", data.phase().name());
        yaml.set("loot-generated", data.lootGenerated());
        yaml.set("rewards", serializeRewards(data.rewards()));
        write(yaml);
    }

    @Override
    public Optional<AirdropData> load() {
        Path file = file();
        if (Files.notExists(file)) return Optional.empty();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        int x = yaml.getInt("position.x");
        int y = yaml.getInt("position.y");
        int z = yaml.getInt("position.z");
        String typeName = yaml.getString("type");
        if (typeName == null) return Optional.empty();
        try {
            AirdropType type = AirdropType.valueOf(typeName);
            AirdropQuality quality = readQuality(yaml);
            AirdropPhase phase = readPhase(yaml);
            UUID id = readId(yaml);
            boolean lootGenerated = yaml.getBoolean("loot-generated", false);
            List<AirdropReward> rewards = lootGenerated ? readRewards(yaml) : List.of();
            return Optional.of(new AirdropData(
                id, new AirdropPosition(x, y, z), type, quality, phase, lootGenerated, rewards));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void clear() {
        try {
            Files.deleteIfExists(file());
        } catch (IOException e) {
            logger.severe("[AirdropStorage] Failed to clear airdrop data: " + e.getMessage());
        }
    }

    /**
     * Loads the persisted epoch-millis deadline of the next automatic drop.
     * Empty when no deadline has been persisted yet (the first interval starts
     * from the moment the automatic system initializes).
     */
    public OptionalLong loadNextDropAtMillis() {
        Path file = file();
        if (Files.notExists(file)) return OptionalLong.empty();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        if (!yaml.contains("next-drop-at")) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(yaml.getLong("next-drop-at"));
    }

    /**
     * Persists the epoch-millis deadline of the next automatic drop so a
     * server restart or plugin reload resumes the countdown instead of
     * resetting it to a full interval.
     */
    public void saveNextDropAtMillis(long epochMillis) {
        YamlConfiguration yaml = readAll();
        yaml.set("next-drop-at", epochMillis);
        write(yaml);
    }

    /**
     * Loads the epoch-millis moment the landed chest must disappear. Empty
     * when no landed drop has a persisted deadline yet.
     */
    public OptionalLong loadDespawnAtMillis() {
        Path file = file();
        if (Files.notExists(file)) {
            return OptionalLong.empty();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        if (!yaml.contains("despawn-at")) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(yaml.getLong("despawn-at"));
    }

    public OptionalLong loadUnlockAtMillis() {
        Path file = file();
        if (Files.notExists(file)) {
            return OptionalLong.empty();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        return yaml.contains("unlock-at")
            ? OptionalLong.of(yaml.getLong("unlock-at"))
            : OptionalLong.empty();
    }

    public void saveUnlockAtMillis(long epochMillis) {
        YamlConfiguration yaml = readAll();
        yaml.set("unlock-at", epochMillis);
        write(yaml);
    }

    /**
     * Persists the epoch-millis moment the landed chest disappears, so the
     * chest countdown and despawn survive a server restart.
     */
    public void saveDespawnAtMillis(long epochMillis) {
        YamlConfiguration yaml = readAll();
        yaml.set("despawn-at", epochMillis);
        write(yaml);
    }

    private AirdropPhase readPhase(YamlConfiguration yaml) {
        String phaseName = yaml.getString("phase");
        if (phaseName != null) {
            return AirdropPhase.valueOf(phaseName);
        }
        return yaml.getBoolean("active") ? AirdropPhase.LANDED : AirdropPhase.CLAIMED;
    }

    private AirdropQuality readQuality(YamlConfiguration yaml) {
        String value = yaml.getString("quality");
        return value == null ? AirdropQuality.COMMON : AirdropQuality.valueOf(value);
    }

    private UUID readId(YamlConfiguration yaml) {
        String value = yaml.getString("id");
        return value == null ? UUID.randomUUID() : UUID.fromString(value);
    }

    private List<Map<String, Object>> serializeRewards(List<AirdropReward> rewards) {
        return rewards.stream().map(reward -> {
            java.util.LinkedHashMap<String, Object> serialized = new java.util.LinkedHashMap<>();
            serialized.put("kind", reward.kind().name());
            serialized.put("item", reward.itemId());
            serialized.put("amount", reward.amount());
            serialized.put("quality", reward.quality().name());
            serialized.put("enchantments", reward.enchantments().stream().map(enchantment -> Map.<String, Object>of(
                "key", enchantment.key(),
                "level", enchantment.level(),
                "max-level", enchantment.maxVanillaLevel(),
                "treasure", enchantment.treasure(),
                "curse", enchantment.curse()
            )).toList());
            return Map.copyOf(serialized);
        }).toList();
    }

    private List<AirdropReward> readRewards(YamlConfiguration yaml) {
        List<AirdropReward> rewards = new ArrayList<>();
        for (Map<?, ?> raw : yaml.getMapList("rewards")) {
            try {
                AirdropRewardKind kind = AirdropRewardKind.valueOf(String.valueOf(raw.get("kind")));
                String item = String.valueOf(raw.get("item"));
                int amount = number(raw.get("amount"), 1);
                AirdropQuality quality = AirdropQuality.valueOf(String.valueOf(raw.get("quality")));
                List<AirdropStoredEnchantment> enchantments = new ArrayList<>();
                Object stored = raw.get("enchantments");
                if (stored instanceof List<?> list) {
                    for (Object element : list) {
                        if (!(element instanceof Map<?, ?> enchantment)) {
                            continue;
                        }
                        enchantments.add(new AirdropStoredEnchantment(
                            String.valueOf(enchantment.get("key")),
                            number(enchantment.get("level"), 1),
                            number(enchantment.get("max-level"), 1),
                            Boolean.parseBoolean(String.valueOf(enchantment.get("treasure"))),
                            Boolean.parseBoolean(String.valueOf(enchantment.get("curse")))
                        ));
                    }
                }
                rewards.add(new AirdropReward(kind, item, amount, quality, enchantments));
            } catch (RuntimeException invalidReward) {
                logger.warning("[AirdropStorage] Ignoring invalid persisted reward: " + invalidReward.getMessage());
            }
        }
        return List.copyOf(rewards);
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    /** Loads the whole file so unrelated keys (the next-drop deadline) survive writes. */
    private YamlConfiguration readAll() {
        Path file = file();
        if (Files.notExists(file)) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file.toFile());
    }

    private void write(YamlConfiguration yaml) {
        try {
            Files.createDirectories(dataFolder);
            yaml.save(file().toFile());
        } catch (IOException e) {
            logger.severe("[AirdropStorage] Failed to save airdrop data: " + e.getMessage());
        }
    }
}
