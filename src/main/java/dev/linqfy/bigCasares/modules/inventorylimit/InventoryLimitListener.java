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
        java.util.Map<Material, Integer> incoming = service.countLimitedItemsInStack(stack);
        for (java.util.Map.Entry<Material, Integer> entry : incoming.entrySet()) {
            if (!service.canAccept(player, entry.getKey(), entry.getValue())) {
                event.setCancelled(true);
                return;
            }
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
        java.util.Map<Material, Integer> incoming = service.countLimitedItemsInStack(candidate);
        if (incoming.isEmpty()) {
            return;
        }

        for (java.util.Map.Entry<Material, Integer> entry : incoming.entrySet()) {
            if (!service.canAccept(player, entry.getKey(), entry.getValue())) {
                event.setCancelled(true);
                service.sendLimitMessage(player, entry.getKey());
                return;
            }
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
            if (!service.countLimitedItemsInStack(newStack).isEmpty()) {
                module.enforceLater(player);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        boolean triggerEnforce = false;
        
        java.util.Map<Material, Integer> mainCounts = service.countLimitedItemsInStack(event.getMainHandItem());
        for (java.util.Map.Entry<Material, Integer> entry : mainCounts.entrySet()) {
            if (!service.canAccept(event.getPlayer(), entry.getKey(), 0)) {
                triggerEnforce = true;
                break;
            }
        }
        
        if (!triggerEnforce) {
            java.util.Map<Material, Integer> offCounts = service.countLimitedItemsInStack(event.getOffHandItem());
            for (java.util.Map.Entry<Material, Integer> entry : offCounts.entrySet()) {
                if (!service.canAccept(event.getPlayer(), entry.getKey(), 0)) {
                    triggerEnforce = true;
                    break;
                }
            }
        }
        
        if (triggerEnforce) {
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
