package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class EchoArrowConversionListener implements Listener {

    private final EchoArrowItem echoArrow;
    private final int maxArrows;
    private final BukkitRuntimeRegistrations registrations;

    public EchoArrowConversionListener(
        EchoArrowItem echoArrow,
        int maxArrows,
        BukkitRuntimeRegistrations registrations
    ) {
        this.echoArrow = echoArrow;
        this.maxArrows = maxArrows;
        this.registrations = registrations;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (stack.getType() != Material.ECHO_SHARD) {
            return;
        }
        int accepted = Math.min(stack.getAmount(), Math.max(0, maxArrows - count(player)));
        if (accepted == 0) {
            event.setCancelled(true);
            player.sendMessage("§cNo podés llevar más de " + maxArrows + " flechas de eco.");
            return;
        }
        int remainder = stack.getAmount() - accepted;
        event.getItem().setItemStack(echoArrow.createItemStack(accepted));
        if (remainder > 0) {
            player.getWorld().dropItemNaturally(event.getItem().getLocation(), new ItemStack(Material.ECHO_SHARD, remainder));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            reconcileNextTick(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            reconcileNextTick(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            reconcile(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        reconcile(event.getPlayer());
    }

    public void reconcile(Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        int current = count(player);
        boolean changed = false;
        for (int index = 0; index < storage.length; index++) {
            ItemStack stack = storage[index];
            if (stack == null || stack.getType() != Material.ECHO_SHARD) {
                continue;
            }
            int accepted = Math.min(stack.getAmount(), Math.max(0, maxArrows - current));
            if (accepted > 0) {
                storage[index] = echoArrow.createItemStack(accepted);
                current += accepted;
            } else {
                storage[index] = null;
            }
            int overflow = stack.getAmount() - accepted;
            if (overflow > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), echoArrow.createItemStack(overflow));
            }
            changed = true;
        }
        if (changed) {
            player.getInventory().setStorageContents(storage);
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() == Material.ECHO_SHARD) {
            int accepted = Math.min(offhand.getAmount(), Math.max(0, maxArrows - current));
            player.getInventory().setItemInOffHand(accepted > 0 ? echoArrow.createItemStack(accepted) : null);
            int overflow = offhand.getAmount() - accepted;
            if (overflow > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), echoArrow.createItemStack(overflow));
            }
        }
    }

    private void reconcileNextTick(Player player) {
        registrations.scheduleDelayed("echo-arrow-reconcile", () -> reconcile(player), 1L);
    }

    private int count(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (echoArrow.matches(stack)) {
                total += stack.getAmount();
            }
        }
        return total;
    }
}
