package com.copperapple.listeners;

import com.copperapple.CopperApplePlugin;
import com.copperapple.items.CustomItem;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ItemConsumeListener implements Listener {

    private final CopperApplePlugin plugin;
    private static final long COOLDOWN_MS = 5_000L;

    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ItemConsumeListener(CopperApplePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {

        if (event.isCancelled()) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        Optional<CustomItem> customItemOpt =
                plugin.getItemRegistry().findByItemStack(item);

        if (customItemOpt.isEmpty()) return;

        Player player = event.getPlayer();

        // Protección spectator
        if (player.getGameMode() == GameMode.SPECTATOR) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        long last = cooldowns.getOrDefault(uuid, 0L);
        if (now - last < COOLDOWN_MS) {
            long remaining = (long) Math.ceil((COOLDOWN_MS - (now - last)) / 1000.0);
            player.sendMessage("§c⏳ Esperá " + remaining + "s para usar otra.");
            return;
        }

        cooldowns.put(uuid, now);

        customItemOpt.get().onConsume(player, item);
    }
}