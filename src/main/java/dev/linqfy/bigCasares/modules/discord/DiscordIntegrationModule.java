package dev.linqfy.bigCasares.modules.discord;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.communication.EmojiAliasService;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.modules.moderation.AuditEvent;
import dev.linqfy.bigCasares.modules.moderation.AuditService;
import dev.linqfy.bigCasares.modules.moderation.AuditSeverity;
import dev.linqfy.bigCasares.modules.servercontrol.ServerControlModule;
import org.bukkit.scheduler.BukkitTask;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class DiscordIntegrationModule implements PluginModule {
    private final BigCasares plugin;
    private final AuditService audit;
    private final ServerControlModule serverControl;
    private final EmojiAliasService emojiAliases;
    private DiscordGateway injectedGateway;
    private DiscordGateway gateway;
    private DiscordSettings settings;
    private BukkitTask reconciliationTask;

    public DiscordIntegrationModule(
        BigCasares plugin,
        AuditService audit,
        ServerControlModule serverControl,
        EmojiAliasService emojiAliases
    ) {
        this(plugin, audit, serverControl, emojiAliases, null);
    }

    public DiscordIntegrationModule(
        BigCasares plugin,
        AuditService audit,
        ServerControlModule serverControl,
        EmojiAliasService emojiAliases,
        DiscordGateway injectedGateway
    ) {
        this.plugin = plugin;
        this.audit = audit;
        this.serverControl = serverControl;
        this.emojiAliases = emojiAliases;
        this.injectedGateway = injectedGateway;
    }

    @Override
    public String getId() {
        return "discord-integration";
    }

    @Override
    public void onEnable() {
        if (plugin == null) {
            return;
        }
        settings = DiscordSettings.load(plugin.getConfig());
        try {
            DiscordFieldValidator.validateConfiguration(plugin.getConfig());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Discord desactivado: presentación inválida: " + ex.getMessage());
            return;
        }
        DiscordSecrets secrets;
        try {
            secrets = DiscordSecrets.loadOrCreate(plugin.getDataFolder().toPath().resolve("discord-secrets.yml"));
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Discord desactivado: no se pudo preparar discord-secrets.yml", ex);
            return;
        }
        if (!settings.hasRequiredIds() || secrets.token().isBlank() || !validWebhook(secrets.bridgeWebhookUrl())) {
            plugin.getLogger().warning(
                "Discord desactivado: completá token y bridge-webhook-url en discord-secrets.yml y todos los IDs en config.yml."
            );
            return;
        }
        if (injectedGateway != null) {
            gateway = injectedGateway;
        } else {
            gateway = new JdaDiscordGateway(
                plugin, settings, secrets, emojiAliases, audit,
                (author, message) -> plugin.getServer().broadcastMessage("§9[Discord] §f" + author + "§7: §f" + message)
            );
        }
        if (audit != null) {
            audit.addSink(gateway);
        }
        if (serverControl != null && serverControl.service() != null) {
            serverControl.setDiscordPlayerRefresh(this::refreshPlayers);
            serverControl.setMinecraftChatBridge((player, message) -> gateway.sendMinecraftChat(
                player.getName(), message, avatarUrl(player.getName(), player.getUniqueId().toString())
            ));
        }
        gateway.start();
        gateway.updateServerStatus(true);
        refreshPlayers();
        reconciliationTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin, this::refreshPlayers,
            settings.reconciliationSeconds() * 20L,
            settings.reconciliationSeconds() * 20L
        );
        if (audit != null) {
            audit.publish(AuditEvent.system(AuditSeverity.INFO, "discord", "integration-enabled", "Integración Discord iniciada"));
        }
    }

    @Override
    public void onDisable() {
        if (reconciliationTask != null) {
            reconciliationTask.cancel();
            reconciliationTask = null;
        }
        if (serverControl != null && serverControl.service() != null) {
            serverControl.setDiscordPlayerRefresh(null);
            serverControl.setMinecraftChatBridge(null);
        }
        if (gateway != null) {
            gateway.updateServerStatus(false);
            if (audit != null) {
                audit.removeSink(gateway);
            }
            gateway.stop();
            gateway = null;
        }
    }

    public DiscordGateway gateway() {
        return gateway;
    }

    public void refreshPlayers() {
        if (gateway == null || serverControl == null || serverControl.service() == null) {
            return;
        }
        gateway.updateVisiblePlayers(serverControl.visiblePlayerNames());
    }

    private String avatarUrl(String playerName, String uuid) {
        String encodedName = URLEncoder.encode(playerName, StandardCharsets.UTF_8);
        return settings.avatarTemplate().replace("%player%", encodedName).replace("%uuid%", uuid);
    }

    private boolean validWebhook(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            java.net.URI uri = java.net.URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && uri.getPath().contains("/api/webhooks/");
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
