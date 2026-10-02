package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.function.BiPredicate;

/** Pure inventory simulation used before an Airdrop can be claimed. */
final class AirdropInventoryCapacity {

    private AirdropInventoryCapacity() {
    }

    static boolean canFit(ItemStack[] contents, List<ItemStack> rewards, int inventoryMaxStackSize) {
        return canFit(contents, rewards, inventoryMaxStackSize,
            ItemStack::getMaxStackSize, ItemStack::isSimilar);
    }

    static boolean canFit(
        ItemStack[] contents,
        List<ItemStack> rewards,
        int inventoryMaxStackSize,
        ToIntFunction<ItemStack> maxStackSizeResolver
    ) {
        return canFit(contents, rewards, inventoryMaxStackSize, maxStackSizeResolver,
            (left, right) -> left.getType() == right.getType());
    }

    static boolean canFit(
        ItemStack[] contents,
        List<ItemStack> rewards,
        int inventoryMaxStackSize,
        ToIntFunction<ItemStack> maxStackSizeResolver,
        BiPredicate<ItemStack, ItemStack> similarity
    ) {
        if (contents == null || rewards == null || inventoryMaxStackSize <= 0) {
            return false;
        }
        Objects.requireNonNull(maxStackSizeResolver, "maxStackSizeResolver");
        Objects.requireNonNull(similarity, "similarity");

        ItemStack[] simulated = Arrays.stream(contents)
            .map(stack -> stack == null ? null : stack.clone())
            .toArray(ItemStack[]::new);

        for (ItemStack reward : rewards) {
            if (isEmpty(reward)) {
                continue;
            }
            int remaining = reward.getAmount();
            int maxStackSize = Math.min(inventoryMaxStackSize, maxStackSizeResolver.applyAsInt(reward));
            if (maxStackSize <= 0) {
                return false;
            }

            for (ItemStack slot : simulated) {
                if (remaining <= 0) {
                    break;
                }
                if (isEmpty(slot) || !similarity.test(slot, reward) || slot.getAmount() >= maxStackSize) {
                    continue;
                }
                int added = Math.min(remaining, maxStackSize - slot.getAmount());
                slot.setAmount(slot.getAmount() + added);
                remaining -= added;
            }

            for (int index = 0; index < simulated.length && remaining > 0; index++) {
                if (!isEmpty(simulated[index])) {
                    continue;
                }
                int added = Math.min(remaining, maxStackSize);
                ItemStack placed = reward.clone();
                placed.setAmount(added);
                simulated[index] = placed;
                remaining -= added;
            }

            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(ItemStack stack) {
        // Avoid Material#isAir here: on the lightweight Spigot test runtime it
        // initializes Bukkit's live registry, which is unavailable without a server.
        return stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0;
    }

}
