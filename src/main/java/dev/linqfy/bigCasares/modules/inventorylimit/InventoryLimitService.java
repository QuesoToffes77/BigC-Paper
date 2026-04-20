package dev.linqfy.bigCasares.modules.inventorylimit;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class InventoryLimitService {

    private final Map<Material, Integer> limits;

    public InventoryLimitService(Map<Material, Integer> limits) {
        this.limits = limits;
    }

    public int count(ItemStack[] contents, Material material) {
        int total = 0;
        for (ItemStack stack : contents) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public int overflow(ItemStack[] contents, Material material) {
        Integer max = limits.get(material);
        if (max == null) {
            return 0;
        }
        return Math.max(0, count(contents, material) - max);
    }

    public boolean canAccept(ItemStack[] contents, Material material, int incomingAmount) {
        Integer max = limits.get(material);
        if (max == null) {
            return true;
        }
        return count(contents, material) + incomingAmount <= max;
    }

    public int trimOverflow(ItemStack[] contents, Material material) {
        int overflow = overflow(contents, material);
        int remaining = overflow;
        if (remaining <= 0) {
            return 0;
        }

        for (int i = contents.length - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material) {
                continue;
            }

            int taken = Math.min(stack.getAmount(), remaining);
            int nextAmount = stack.getAmount() - taken;
            remaining -= taken;

            if (nextAmount <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(nextAmount);
                contents[i] = stack;
            }
        }

        return overflow - remaining;
    }

    public boolean isLimited(Material material) {
        return limits.containsKey(material);
    }

    public boolean canAccept(Player player, Material material, int incomingAmount) {
        return canAccept(snapshot(player.getInventory()), material, incomingAmount);
    }

    public int enforcePlayerInventory(Player player) {
        ItemStack[] contents = snapshot(player.getInventory());
        int totalRemoved = 0;
        for (Material material : limits.keySet()) {
            int removed = trimOverflow(contents, material);
            if (removed <= 0) {
                continue;
            }
            totalRemoved += removed;
            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(material, removed));
        }
        applySnapshot(player.getInventory(), contents);
        return totalRemoved;
    }

    public void sendLimitMessage(Player player, Material material) {
        Integer max = limits.get(material);
        if (max != null) {
            player.sendMessage("§cNo podes tener mas de " + max + " " + material.name() + " en el inventario.");
        }
    }

    public Map<Material, Integer> limits() {
        return limits;
    }

    private ItemStack[] snapshot(PlayerInventory inventory) {
        ItemStack[] storage = inventory.getStorageContents();
        ItemStack[] contents = new ItemStack[storage.length + 1];
        System.arraycopy(storage, 0, contents, 0, storage.length);
        contents[storage.length] = inventory.getItemInOffHand();
        return contents;
    }

    private void applySnapshot(PlayerInventory inventory, ItemStack[] contents) {
        int storageLength = inventory.getStorageContents().length;
        ItemStack[] storage = new ItemStack[storageLength];
        System.arraycopy(contents, 0, storage, 0, storageLength);
        inventory.setStorageContents(storage);
        inventory.setItemInOffHand(contents[storageLength]);
    }
}
