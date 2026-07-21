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
        int incoming = countInStack(stack);
        if (incoming > 0 && !canAccept(player, incoming)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !targetsPlayerInventory(event.getClickedInventory(), player)) {
            return;
        }

        ItemStack candidate = resolveIncomingStack(event);
        int incoming = countInStack(candidate);
        if (incoming == 0) {
            return;
        }
        if (!canAccept(player, incoming)) {
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
            if (stack == null) continue;

            if (crossbowData.isEchoChargedCrossbow(stack)) {
                storage[i] = null;
                remaining--;
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            } else if (stack.hasItemMeta() && stack.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
                java.util.List<ItemStack> bundledItems = new java.util.ArrayList<>(bundleMeta.getItems());
                boolean changed = false;
                for (int j = bundledItems.size() - 1; j >= 0 && remaining > 0; j--) {
                    ItemStack bStack = bundledItems.get(j);
                    if (crossbowData.isEchoChargedCrossbow(bStack)) {
                        bundledItems.remove(j);
                        remaining--;
                        player.getWorld().dropItemNaturally(player.getLocation(), bStack);
                        changed = true;
                    }
                }
                if (changed) {
                    bundleMeta.setItems(bundledItems);
                    stack.setItemMeta(bundleMeta);
                    storage[i] = stack;
                }
            }
        }
        player.getInventory().setStorageContents(storage);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (remaining > 0 && offhand != null) {
            if (crossbowData.isEchoChargedCrossbow(offhand)) {
                player.getInventory().setItemInOffHand(null);
                remaining--;
                player.getWorld().dropItemNaturally(player.getLocation(), offhand);
            } else if (offhand.hasItemMeta() && offhand.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
                java.util.List<ItemStack> bundledItems = new java.util.ArrayList<>(bundleMeta.getItems());
                boolean changed = false;
                for (int j = bundledItems.size() - 1; j >= 0 && remaining > 0; j--) {
                    ItemStack bStack = bundledItems.get(j);
                    if (crossbowData.isEchoChargedCrossbow(bStack)) {
                        bundledItems.remove(j);
                        remaining--;
                        player.getWorld().dropItemNaturally(player.getLocation(), bStack);
                        changed = true;
                    }
                }
                if (changed) {
                    bundleMeta.setItems(bundledItems);
                    offhand.setItemMeta(bundleMeta);
                    player.getInventory().setItemInOffHand(offhand);
                }
            }
        }
        return overflow - remaining;
    }

    public int countEchoChargedCrossbows(Player player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            total += countInStack(stack);
        }
        total += countInStack(player.getInventory().getItemInOffHand());
        return total;
    }

    private int countInStack(ItemStack stack) {
        if (stack == null) return 0;
        int total = 0;
        if (crossbowData.isEchoChargedCrossbow(stack)) {
            total += Math.max(1, stack.getAmount());
        } else if (stack.hasItemMeta() && stack.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
            for (ItemStack bStack : bundleMeta.getItems()) {
                if (crossbowData.isEchoChargedCrossbow(bStack)) {
                    total += Math.max(1, bStack.getAmount());
                }
            }
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
