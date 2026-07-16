package dev.linqfy.bigCasares.modules.discord;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.communication.EmojiAliasService;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
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
    private RuntimeRegistrationScope compatibilityScope;
    private boolean gatewayStopped = true;

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
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        if (plugin == null) {
            return;
        }
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        scope.register("module-state", this::clearRuntimeState);
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
                (author, message) -> plugin.getServer().broadcastMessage(
                    "§9[Discord] §f" + author + "§7: §f" + message),
                scope.generation()::isActive
            );
        }
        DiscordGateway ownedGateway = gateway;
        gatewayStopped = false;
        if (audit != null) {
            audit.addSink(ownedGateway);
            scope.register("discord-audit-sink", () -> audit.removeSink(ownedGateway));
        }
        if (serverControl != null && serverControl.service() != null) {
            serverControl.setDiscordPlayerRefresh(this::refreshPlayers);
            serverControl.setMinecraftChatBridge((player, message) -> gateway.sendMinecraftChat(
                player.getName(), message, avatarUrl(player.getName(), player.getUniqueId().toString())
            ));
            scope.register("server-control-bridges", () -> {
                serverControl.setDiscordPlayerRefresh(null);
                serverControl.setMinecraftChatBridge(null);
            });
        }
        ownedGateway.start();
        scope.register("discord-gateway", () -> stopGateway(ownedGateway));
        ownedGateway.updateServerStatus(true);
        refreshPlayers();
        reconciliationTask = registrations.scheduleRepeating(
            "discord-reconciliation", this::refreshPlayers,
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
            stopGateway(gateway);
            gateway = null;
        }
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
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

    private void clearRuntimeState() {
        reconciliationTask = null;
        gateway = null;
        gatewayStopped = true;
        settings = null;
    }

    private void stopGateway(DiscordGateway ownedGateway) {
        if (gatewayStopped) {
            return;
        }
        gatewayStopped = true;
        ownedGateway.stop();
    }
}
