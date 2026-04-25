package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CustomCrossbowInventoryLimitListener implements Listener {

    private final CustomCrossbowData crossbowData;
    private final CustomCrossbowSettings settings;

    public CustomCrossbowInventoryLimitListener(CustomCrossbowData crossbowData, CustomCrossbowSettings settings) {
        this.crossbowData = crossbowData;
        this.settings = settings;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack stack = event.getItem().getItemStack();
        if (crossbowData.isEchoChargedCrossbow(stack) && !canAccept(player, 1)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !targetsPlayerInventory(event.getClickedInventory(), player)) {
            return;
        }

        ItemStack candidate = resolveIncomingStack(event);
        if (!crossbowData.isEchoChargedCrossbow(candidate)) {
            return;
        }
        if (!canAccept(player, candidate.getAmount())) {
            event.setCancelled(true);
            player.sendMessage("§cNo podes tener mas de " + settings.maxEchoChargedCrossbows() + " crossbows cargadas con Echo Shard.");
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        enforce(event.getPlayer());
    }

    public boolean canAccept(Player player, int incomingAmount) {
        return countEchoChargedCrossbows(player) + incomingAmount <= settings.maxEchoChargedCrossbows();
    }

    public int enforce(Player player) {
        int overflow = Math.max(0, countEchoChargedCrossbows(player) - settings.maxEchoChargedCrossbows());
        int remaining = overflow;
        if (remaining <= 0) {
            return 0;
        }

        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = storage.length - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = storage[i];
            if (!crossbowData.isEchoChargedCrossbow(stack)) {
                continue;
            }
            storage[i] = null;
            remaining--;
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.getInventory().setStorageContents(storage);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (remaining > 0 && crossbowData.isEchoChargedCrossbow(offhand)) {
            player.getInventory().setItemInOffHand(null);
            remaining--;
            player.getWorld().dropItemNaturally(player.getLocation(), offhand);
        }
        return overflow - remaining;
    }

    public int countEchoChargedCrossbows(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (crossbowData.isEchoChargedCrossbow(stack)) {
                total += Math.max(1, stack.getAmount());
            }
        }
        if (crossbowData.isEchoChargedCrossbow(player.getInventory().getItemInOffHand())) {
            total++;
        }
        return total;
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
}
