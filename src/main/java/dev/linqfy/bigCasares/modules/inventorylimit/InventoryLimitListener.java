package dev.linqfy.bigCasares.modules.inventorylimit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class InventoryLimitListener implements Listener {

    private final InventoryLimitModule module;
    private final InventoryLimitService service;

    public InventoryLimitListener(InventoryLimitModule module, InventoryLimitService service) {
        this.module = module;
        this.service = service;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (service.isLimited(stack.getType()) && !service.canAccept(player, stack.getType(), stack.getAmount())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!targetsPlayerInventory(event.getClickedInventory(), player)) {
            return;
        }

        ItemStack candidate = resolveIncomingStack(event);
        if (candidate == null || !service.isLimited(candidate.getType())) {
            return;
        }

        if (!service.canAccept(player, candidate.getType(), candidate.getAmount())) {
            event.setCancelled(true);
            service.sendLimitMessage(player, candidate.getType());
            return;
        }

        module.enforceLater(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot >= player.getInventory().getSize()) {
                continue;
            }
            ItemStack newStack = event.getNewItems().get(rawSlot);
            if (newStack != null && service.isLimited(newStack.getType())) {
                module.enforceLater(player);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Material mainType = typeOf(event.getMainHandItem());
        Material offType = typeOf(event.getOffHandItem());
        if ((service.isLimited(mainType) && !service.canAccept(event.getPlayer(), mainType, 0))
            || (service.isLimited(offType) && !service.canAccept(event.getPlayer(), offType, 0))) {
            module.enforceLater(event.getPlayer());
        }
    }

    private boolean targetsPlayerInventory(Inventory clicked, Player player) {
        return clicked != null && clicked.getType() == InventoryType.PLAYER && clicked.equals(player.getInventory());
    }

    private ItemStack resolveIncomingStack(InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return event.getCurrentItem();
        }
        return event.getCursor();
    }

    private Material typeOf(ItemStack stack) {
        return stack == null ? Material.AIR : stack.getType();
    }
}
