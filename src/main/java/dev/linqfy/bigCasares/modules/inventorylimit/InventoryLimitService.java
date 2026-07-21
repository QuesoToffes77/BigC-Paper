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
            if (stack == null) continue;
            if (stack.getType() == material) {
                total += stack.getAmount();
            } else if (stack.getType() == Material.BUNDLE && stack.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
                for (ItemStack bStack : bundleMeta.getItems()) {
                    if (bStack.getType() == material) {
                        total += bStack.getAmount();
                    }
                }
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

        // First pass: normal stacks
        for (int i = contents.length - 1; i >= 0 && remaining > 0; i--) {
            ItemStack stack = contents[i];
            if (stack == null) continue;

            if (stack.getType() == material) {
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
        }

        // Second pass: inside bundles
        if (remaining > 0) {
            for (int i = contents.length - 1; i >= 0 && remaining > 0; i--) {
                ItemStack stack = contents[i];
                if (stack == null) continue;

                if (stack.getType() == Material.BUNDLE && stack.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
                    java.util.List<ItemStack> bundledItems = new java.util.ArrayList<>(bundleMeta.getItems());
                    boolean changed = false;
                    for (int j = bundledItems.size() - 1; j >= 0 && remaining > 0; j--) {
                        ItemStack bStack = bundledItems.get(j);
                        if (bStack.getType() == material) {
                            int taken = Math.min(bStack.getAmount(), remaining);
                            int nextAmount = bStack.getAmount() - taken;
                            remaining -= taken;

                            if (nextAmount <= 0) {
                                bundledItems.remove(j);
                            } else {
                                bStack.setAmount(nextAmount);
                                bundledItems.set(j, bStack);
                            }
                            changed = true;
                        }
                    }
                    if (changed) {
                        bundleMeta.setItems(bundledItems);
                        stack.setItemMeta(bundleMeta);
                        contents[i] = stack;
                    }
                }
            }
        }

        return overflow - remaining;
    }

    public boolean isLimited(Material material) {
        return limits.containsKey(material);
    }

    public Map<Material, Integer> countLimitedItemsInStack(ItemStack stack) {
        Map<Material, Integer> counts = new java.util.HashMap<>();
        if (stack == null) return counts;
        
        if (isLimited(stack.getType())) {
            counts.put(stack.getType(), stack.getAmount());
        } else if (stack.getType() == Material.BUNDLE && stack.getItemMeta() instanceof org.bukkit.inventory.meta.BundleMeta bundleMeta) {
            for (ItemStack bStack : bundleMeta.getItems()) {
                if (isLimited(bStack.getType())) {
                    counts.put(bStack.getType(), counts.getOrDefault(bStack.getType(), 0) + bStack.getAmount());
                }
            }
        }
        return counts;
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
