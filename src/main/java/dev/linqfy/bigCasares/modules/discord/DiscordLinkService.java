package dev.linqfy.bigCasares.modules.discord;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordLinkService {

    private final File dataFile;
    private final YamlConfiguration config;
    private final Map<UUID, Long> linkedAccounts = new ConcurrentHashMap<>();
    
    // Map of Code -> UUID
    private final Map<String, UUID> pendingCodes = new ConcurrentHashMap<>();
    private final Map<UUID, String> reversePending = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public DiscordLinkService(Plugin plugin) {
        this.dataFile = new File(plugin.getDataFolder().toPath().resolve("data").resolve("discord-integration").toFile(), "discord-links.yml");
        if (!this.dataFile.getParentFile().exists()) {
            this.dataFile.getParentFile().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(dataFile);
        load();
    }

    private void load() {
        if (config.contains("links")) {
            for (String key : config.getConfigurationSection("links").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long discordId = config.getLong("links." + key);
                    linkedAccounts.put(uuid, discordId);
                } catch (IllegalArgumentException e) {
                    // Ignore invalid UUIDs
                }
            }
        }
    }

    private void save() {
        config.set("links", null);
        for (Map.Entry<UUID, Long> entry : linkedAccounts.entrySet()) {
            config.set("links." + entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String generateCode(UUID playerUuid) {
        if (reversePending.containsKey(playerUuid)) {
            String oldCode = reversePending.remove(playerUuid);
            pendingCodes.remove(oldCode);
        }
        
        String code;
        do {
            code = String.format("%06d", random.nextInt(1000000));
        } while (pendingCodes.containsKey(code));

        pendingCodes.put(code, playerUuid);
        reversePending.put(playerUuid, code);
        return code;
    }

    public boolean verifyCode(String code, long discordUserId) {
        UUID playerUuid = pendingCodes.remove(code);
        if (playerUuid != null) {
            reversePending.remove(playerUuid);
            linkedAccounts.put(playerUuid, discordUserId);
            save();
            return true;
        }
        return false;
    }

    public boolean isLinked(UUID playerUuid) {
        return linkedAccounts.containsKey(playerUuid);
    }

    public Optional<Long> getDiscordId(UUID playerUuid) {
        return Optional.ofNullable(linkedAccounts.get(playerUuid));
    }
}
