package dev.linqfy.bigCasares.modules.tombstone;

import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public final class TombstoneListener implements Listener {
    private final TombstoneRuntime runtime;
    private final BukkitRuntimeRegistrations registrations;

    public TombstoneListener(TombstoneRuntime runtime, BukkitRuntimeRegistrations registrations) {
        this.runtime = runtime;
        this.registrations = registrations;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> items = snapshot(player);
        
        StringBuilder logMessage = new StringBuilder("Inventario de " + player.getName() + " antes de morir:\n");
        for (ItemStack item : items) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                logMessage.append("- ").append(item.getAmount()).append("x ").append(item.getType().name()).append("\n");
            }
        }
        org.bukkit.Bukkit.getLogger().info(logMessage.toString());
        
        org.bukkit.Location loc = player.getLocation();
        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Has muerto en las coordenadas: " + 
            org.bukkit.ChatColor.RED + "X: " + loc.getBlockX() + " Y: " + loc.getBlockY() + " Z: " + loc.getBlockZ());

        event.getDrops().clear();
        event.setKeepInventory(false);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setItemOnCursor(null);
        runtime.create(player.getUniqueId(), player.getName(), player.getLocation(), items);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        UUID id = runtime.tombstoneId(event.getRightClicked());
        if (id == null) return;
        event.setCancelled(true);
        runtime.open(id, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        reconcileNextTick(event.getView().getTopInventory().getHolder());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSelectionClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TombstoneSelectionHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        holder.tombstoneAt(event.getRawSlot()).ifPresent(id -> registrations.scheduleDelayed(
            "tombstone-selection-open", () -> runtime.open(id, player), 1L
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        reconcileNextTick(event.getView().getTopInventory().getHolder());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSelectionDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TombstoneSelectionHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof TombstoneInventoryHolder holder) {
            runtime.reconcile(holder.tombstoneId());
        }
    }

    private void reconcileNextTick(org.bukkit.inventory.InventoryHolder holder) {
        if (holder instanceof TombstoneInventoryHolder tombstone) {
            registrations.scheduleDelayed(
                "tombstone-inventory-reconcile", () -> runtime.reconcile(tombstone.tombstoneId()), 1L
            );
        }
    }

    private List<ItemStack> snapshot(Player player) {
        return TombstoneItemLayout.snapshot(
            player.getInventory().getStorageContents(),
            player.getInventory().getArmorContents(),
            player.getInventory().getItemInOffHand(),
            player.getItemOnCursor()
        );
    }
}
