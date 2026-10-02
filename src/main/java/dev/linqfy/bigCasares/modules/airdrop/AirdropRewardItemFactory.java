package dev.linqfy.bigCasares.modules.airdrop;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

final class AirdropRewardItemFactory {
    private final BiFunction<String, Integer, ItemStack> customItemFactory;
    private final Map<String, Material> materials;

    AirdropRewardItemFactory(CustomItemRegistry customItems, Map<String, Material> materials) {
        this(Objects.requireNonNull(customItems, "customItems")::createItemStack, materials);
    }

    AirdropRewardItemFactory(
        BiFunction<String, Integer, ItemStack> customItemFactory,
        Map<String, Material> materials
    ) {
        this.customItemFactory = Objects.requireNonNull(customItemFactory, "customItemFactory");
        this.materials = Map.copyOf(Objects.requireNonNull(materials, "materials"));
    }

    List<ItemStack> createAll(List<AirdropReward> rewards) {
        List<ItemStack> stacks = new ArrayList<>();
        for (AirdropReward reward : rewards) {
            ItemStack generated = create(reward);
            int maximum = Math.max(1, generated.getMaxStackSize());
            int remaining = generated.getAmount();
            while (remaining > 0) {
                ItemStack split = generated.clone();
                split.setAmount(Math.min(maximum, remaining));
                stacks.add(split);
                remaining -= split.getAmount();
            }
        }
        return List.copyOf(stacks);
    }

    ItemStack create(AirdropReward reward) {
        return switch (reward.kind()) {
            case CUSTOM_ITEM -> customItemFactory.apply(reward.itemId(), reward.amount());
            case MATERIAL -> new ItemStack(requireMaterial(reward.itemId()), reward.amount());
            case ENCHANTED_BOOK -> createBook(reward);
        };
    }

    private ItemStack createBook(AirdropReward reward) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        if (!(item.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            throw new IllegalStateException("ENCHANTED_BOOK did not provide EnchantmentStorageMeta");
        }
        for (AirdropStoredEnchantment stored : reward.enchantments()) {
            var enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(stored.key()));
            if (enchantment == null) {
                throw new IllegalStateException("Unknown airdrop enchantment: " + stored.key());
            }
            if (stored.level() > enchantment.getMaxLevel()) {
                throw new IllegalStateException("Illegal level for " + stored.key() + ": " + stored.level());
            }
            meta.addStoredEnchant(enchantment, stored.level(), false);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material requireMaterial(String id) {
        Material material = materials.get(id);
        if (material == null) {
            throw new IllegalStateException("Unvalidated airdrop material: " + id);
        }
        return material;
    }
}
