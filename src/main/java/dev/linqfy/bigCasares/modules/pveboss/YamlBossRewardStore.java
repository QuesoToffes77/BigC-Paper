package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class YamlBossRewardStore implements BossRewardStore {

    private final Path file;
    private final Map<UUID, PendingBossReward> rewards = new LinkedHashMap<>();

    public YamlBossRewardStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        load();
    }

    @Override
    public synchronized void save(PendingBossReward reward) {
        Objects.requireNonNull(reward, "reward");
        rewards.put(reward.rewardId(), reward);
        persist();
    }

    @Override
    public synchronized List<PendingBossReward> pending(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return rewards.values().stream()
            .filter(reward -> reward.playerId().equals(playerId))
            .toList();
    }

    @Override
    public synchronized void remove(UUID rewardId) {
        Objects.requireNonNull(rewardId, "rewardId");
        if (rewards.remove(rewardId) != null) {
            persist();
        }
    }

    private void load() {
        if (!Files.isRegularFile(file)) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        ConfigurationSection root = yaml.getConfigurationSection("pending");
        if (root == null) {
            return;
        }
        for (String rawId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rawId);
            if (section == null) continue;
            UUID rewardId = UUID.fromString(rawId);
            UUID playerId = UUID.fromString(Objects.requireNonNull(section.getString("player-id"), "player-id"));
            String itemId = Objects.requireNonNull(section.getString("item-id"), "item-id");
            int amount = section.getInt("amount");
            rewards.put(rewardId, new PendingBossReward(rewardId, playerId, new BossReward(itemId, amount)));
        }
    }

    private void persist() {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            YamlConfiguration yaml = new YamlConfiguration();
            for (PendingBossReward pending : rewards.values()) {
                String path = "pending." + pending.rewardId();
                yaml.set(path + ".player-id", pending.playerId().toString());
                yaml.set(path + ".item-id", pending.reward().itemId());
                yaml.set(path + ".amount", pending.reward().amount());
            }
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            yaml.save(temporary.toFile());
            replace(temporary);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not persist pending boss rewards to " + file, exception);
        }
    }

    private void replace(Path temporary) throws IOException {
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
