package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

public final class CopperAppleConsumeListener implements Listener {

    private final Predicate<ItemStack> matches;
    private final BiConsumer<Player, ItemStack> consume;
    private final LongSupplier clock;
    private final long cooldownMs;
    private final Map<UUID, Long> cooldownByPlayer = new HashMap<>();

    public CopperAppleConsumeListener(CopperAppleItem copperAppleItem, long cooldownMs) {
        this(copperAppleItem::matches, copperAppleItem::onConsume, cooldownMs, System::currentTimeMillis);
    }

    CopperAppleConsumeListener(Predicate<ItemStack> matches, BiConsumer<Player, ItemStack> consume,
                               long cooldownMs, LongSupplier clock) {
        this.matches = java.util.Objects.requireNonNull(matches, "matches");
        this.consume = java.util.Objects.requireNonNull(consume, "consume");
        this.clock = java.util.Objects.requireNonNull(clock, "clock");
        this.cooldownMs = Math.max(0L, cooldownMs);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onValidateConsume(PlayerItemConsumeEvent event) {
        if (event.isCancelled() || !matches.test(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            event.setCancelled(true);
            return;
        }

        if (cooldownMs > 0L) {
            UUID playerId = player.getUniqueId();
            long now = clock.getAsLong();
            Long lastUse = cooldownByPlayer.get(playerId);
            long elapsed = lastUse == null ? cooldownMs : Math.max(0L, now - lastUse);

            if (elapsed < cooldownMs) {
                long secondsRemaining = (long) Math.ceil((cooldownMs - elapsed) / 1000.0);
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Espera " + secondsRemaining
                    + "s antes de consumir otra Manzana de Cobre.");
                return;
            }

        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        if (event.isCancelled() || !matches.test(event.getItem())) {
            return;
        }
        consume.accept(event.getPlayer(), event.getItem());
        if (cooldownMs > 0L) {
            cooldownByPlayer.put(event.getPlayer().getUniqueId(), clock.getAsLong());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cooldownByPlayer.remove(event.getPlayer().getUniqueId());
    }
}
