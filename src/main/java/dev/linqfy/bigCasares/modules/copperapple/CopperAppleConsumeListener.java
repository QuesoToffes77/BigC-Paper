package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.items.CustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CopperAppleConsumeListener implements Listener {

    private final CustomItemRegistry itemRegistry;
    private final long cooldownMs;
    private final Map<UUID, Long> cooldownByPlayer = new HashMap<>();

    public CopperAppleConsumeListener(CustomItemRegistry itemRegistry, long cooldownMs) {
        this.itemRegistry = itemRegistry;
        this.cooldownMs = Math.max(0L, cooldownMs);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Optional<CustomItem> customItem = itemRegistry.findByItemStack(event.getItem());
        if (customItem.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (cooldownMs > 0L) {
            UUID playerId = player.getUniqueId();
            long now = System.currentTimeMillis();
            long lastUse = cooldownByPlayer.getOrDefault(playerId, 0L);
            long elapsed = now - lastUse;

            if (elapsed < cooldownMs) {
                long secondsRemaining = (long) Math.ceil((cooldownMs - elapsed) / 1000.0);
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Wait " + secondsRemaining + "s before consuming another Copper Apple.");
                return;
            }

            cooldownByPlayer.put(playerId, now);
        }

        customItem.get().onConsume(player, event.getItem());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownByPlayer.remove(event.getPlayer().getUniqueId());
    }
}
