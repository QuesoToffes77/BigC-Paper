package dev.linqfy.bigCasares.modules.servercontrol;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.modules.moderation.AuditEvent;
import dev.linqfy.bigCasares.modules.moderation.AuditSeverity;
import dev.linqfy.bigCasares.modules.moderation.AuditSink;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Optional;
import java.util.UUID;

public final class VanishManager {
    private final BigCasares plugin;
    private final VanishSessionRegistry registry;
    private final ServerControlSettings settings;
    private final AuditSink audit;
    private Runnable playerListRefresh = () -> { };

    public VanishManager(
        BigCasares plugin,
        VanishSessionRegistry registry,
        ServerControlSettings settings,
        AuditSink audit
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.settings = settings;
        this.audit = audit == null ? ignored -> { } : audit;
    }

    public boolean isVanished(Player player) {
        return player != null && isVanished(player.getUniqueId());
    }

    public boolean isVanished(UUID playerId) {
        return registry.contains(playerId);
    }

    public boolean toggle(Player player) {
        if (isVanished(player)) {
            exit(player, true);
            return false;
        }
        enter(player, true);
        return true;
    }

    public void enter(Player player, boolean announce) {
        if (!registry.contains(player.getUniqueId())) {
            registry.put(player.getUniqueId(), new VanishSessionRegistry.VanishSnapshot(
                player.isInvisible(), player.isSilent(), player.isCollidable(), player.getCanPickupItems(),
                player.isVisibleByDefault(),
                player.getPotionEffect(PotionEffectType.INVISIBILITY)
            ));
        }
        applyHiddenState(player);
        hideFromAllViewers(player);
        if (announce) {
            plugin.getServer().broadcastMessage(settings.fakeLeaveMessage().replace("%player%", player.getName()));
            player.sendMessage("§aAhora estás en vanish.");
            audit.publish(AuditEvent.player(
                AuditSeverity.INFO, "control", "vanish-enabled", player.getUniqueId(), player.getName(),
                player.getName() + " activó vanish"
            ));
        }
        playerListRefresh.run();
    }

    public void exit(Player player, boolean announce) {
        VanishSessionRegistry.VanishSnapshot snapshot = registry.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        restoreState(player, snapshot);
        showToAllViewers(player);
        if (announce) {
            plugin.getServer().broadcastMessage(settings.fakeJoinMessage().replace("%player%", player.getName()));
            player.sendMessage("§aVanish desactivado.");
            audit.publish(AuditEvent.player(
                AuditSeverity.INFO, "control", "vanish-disabled", player.getUniqueId(), player.getName(),
                player.getName() + " desactivó vanish"
            ));
        }
        playerListRefresh.run();
    }

    public void handleViewerJoin(Player viewer) {
        for (UUID vanishedId : registry.playerIds()) {
            Player vanished = plugin.getServer().getPlayer(vanishedId);
            if (vanished != null && vanished.isOnline()) {
                viewer.hidePlayer(plugin, vanished);
                viewer.unlistPlayer(vanished);
            }
        }
        if (registry.contains(viewer.getUniqueId())) {
            enter(viewer, false);
        }
    }

    public boolean handleDisconnect(Player player) {
        if (!isVanished(player)) {
            return false;
        }
        VanishSessionRegistry.VanishSnapshot snapshot = registry.remove(player.getUniqueId());
        if (snapshot != null) {
            restoreState(player, snapshot);
            showToAllViewers(player);
        }
        playerListRefresh.run();
        return true;
    }

    public void recoverOnlineSessions() {
        for (UUID playerId : registry.playerIds()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                applyHiddenState(player);
                hideFromAllViewers(player);
            }
        }
    }

    public void clearOnlineSessions() {
        for (UUID playerId : registry.playerIds()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                exit(player, false);
            } else {
                registry.remove(playerId);
            }
        }
    }

    public void setPlayerListRefresh(Runnable playerListRefresh) {
        this.playerListRefresh = playerListRefresh == null ? () -> { } : playerListRefresh;
    }

    private void hideFromAllViewers(Player vanished) {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            viewer.hidePlayer(plugin, vanished);
            viewer.unlistPlayer(vanished);
        }
    }

    private void showToAllViewers(Player player) {
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            viewer.showPlayer(plugin, player);
            viewer.listPlayer(player);
        }
    }

    private void applyHiddenState(Player player) {
        player.setInvisible(true);
        player.setSilent(true);
        player.setCollidable(false);
        player.setCanPickupItems(false);
        player.setVisibleByDefault(false);
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false
        ), true);
    }

    private void restoreState(Player player, VanishSessionRegistry.VanishSnapshot snapshot) {
        player.setInvisible(snapshot.invisible());
        player.setSilent(snapshot.silent());
        player.setCollidable(snapshot.collidable());
        player.setCanPickupItems(snapshot.canPickupItems());
        player.setVisibleByDefault(snapshot.visibleByDefault());
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Optional.ofNullable(snapshot.invisibilityEffect()).ifPresent(effect -> player.addPotionEffect(effect, true));
    }
}
