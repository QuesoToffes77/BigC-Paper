package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class CopperAppleOxidationListener implements Listener {

    private final CopperAppleOxidationService oxidationService;

    public CopperAppleOxidationListener(CopperAppleOxidationService oxidationService) {
        this.oxidationService = oxidationService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        oxidationService.refreshInventory(event.getPlayer().getInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        oxidationService.refreshInventory(event.getInventory());
        if (event.getPlayer() instanceof Player player) {
            oxidationService.refreshInventory(player.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        oxidationService.normalizeForMerge(event.getCurrentItem(), event.getCursor());
        oxidationService.normalizeWithInventory(event.getCurrentItem(), event.getView().getTopInventory());
        oxidationService.normalizeWithInventory(event.getCurrentItem(), event.getView().getBottomInventory());
        oxidationService.normalizeWithInventory(event.getCursor(), event.getView().getTopInventory());
        oxidationService.normalizeWithInventory(event.getCursor(), event.getView().getBottomInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        oxidationService.normalizeWithInventory(event.getOldCursor(), event.getView().getTopInventory());
        oxidationService.normalizeWithInventory(event.getOldCursor(), event.getView().getBottomInventory());
        event.getNewItems().values().forEach(stack -> {
            oxidationService.normalizeWithInventory(stack, event.getView().getTopInventory());
            oxidationService.normalizeWithInventory(stack, event.getView().getBottomInventory());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            oxidationService.normalizeWithInventory(event.getItem().getItemStack(), player.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        oxidationService.normalizeWithInventory(event.getItem().getItemStack(), event.getInventory());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        oxidationService.normalizeWithInventory(event.getItem(), event.getDestination());
    }
}
