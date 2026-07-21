package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.items.CustomItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public final class CustomArrowLimitListener implements Listener {
    private final EchoArrowItem echoArrow;
    private final GoldenTippedAmethystArrowItem amethystArrow;
    private final CustomCrossbowSettings settings;

    public CustomArrowLimitListener(
        EchoArrowItem echoArrow,
        GoldenTippedAmethystArrowItem amethystArrow,
        CustomCrossbowSettings settings
    ) {
        this.echoArrow = echoArrow;
        this.amethystArrow = amethystArrow;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack stack = event.getItem().getItemStack();
        CustomItem item = limitedItem(stack);
        if (item != null && count(player, item) + stack.getAmount() > maximum(item)) {
            event.setCancelled(true);
            sendLimit(player, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe().getResult();
        CustomItem item = limitedItem(result);
        if (item != null && count(player, item) + result.getAmount() > maximum(item)) {
            event.setCancelled(true);
            sendLimit(player, item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack incoming = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
            ? event.getCurrentItem() : event.getCursor();
        CustomItem item = limitedItem(incoming);
        if (item != null && count(player, item) + incoming.getAmount() > maximum(item)
            && event.getClickedInventory() != player.getInventory()) {
            event.setCancelled(true);
            sendLimit(player, item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        enforce(event.getPlayer(), echoArrow);
        enforce(event.getPlayer(), amethystArrow);
    }

    private void enforce(Player player, CustomItem item) {
        int overflow = Math.max(0, count(player, item) - maximum(item));
        if (overflow == 0) return;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = contents.length - 1; slot >= 0 && overflow > 0; slot--) {
            ItemStack stack = contents[slot];
            if (!item.matches(stack)) continue;
            int removed = Math.min(overflow, stack.getAmount());
            stack.setAmount(stack.getAmount() - removed);
            contents[slot] = stack.getAmount() == 0 ? null : stack;
            overflow -= removed;
            player.getWorld().dropItemNaturally(player.getLocation(), item.createItemStack(removed));
        }
        player.getInventory().setStorageContents(contents);
    }

    private CustomItem limitedItem(ItemStack stack) {
        if (echoArrow.matches(stack)) return echoArrow;
        if (amethystArrow.matches(stack)) return amethystArrow;
        return null;
    }

    private int maximum(CustomItem item) {
        return item == echoArrow ? settings.maxEchoArrows() : settings.maxAmethystArrows();
    }

    private int count(Player player, CustomItem item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (item.matches(stack)) total += stack.getAmount();
        }
        return total;
    }

    private void sendLimit(Player player, CustomItem item) {
        String name = item == echoArrow ? "flechas de eco" : "flechas de amatista con punta de oro";
        player.sendMessage("§cNo podés llevar más de " + maximum(item) + " " + name + ".");
    }
}
