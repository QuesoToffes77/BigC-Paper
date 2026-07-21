package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PaperSahurRewardDelivery implements SahurRewardService.DeliveryGateway {

    private final CustomItemRegistry customItems;

    public PaperSahurRewardDelivery(CustomItemRegistry customItems) {
        this.customItems = Objects.requireNonNull(customItems, "customItems");
    }

    @Override
    public boolean isOnline(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null && player.isOnline();
    }

    @Override
    public void giveOrDrop(UUID playerId, BossReward reward) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            throw new IllegalStateException("reward player is offline: " + playerId);
        }
        ItemStack stack = createStack(reward, customItems);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        for (ItemStack remainder : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), remainder);
        }
    }

    static ItemStack createStack(BossReward reward, CustomItemRegistry customItems) {
        Objects.requireNonNull(reward, "reward");
        Objects.requireNonNull(customItems, "customItems");
        if (reward.itemId().equals("sahurs_bat")) {
            return customItems.createItemStack(reward.itemId(), reward.amount());
        }
        Material material = Material.matchMaterial(reward.itemId());
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("invalid vanilla reward material: " + reward.itemId());
        }
        return new ItemStack(material, reward.amount());
    }

    static RewardStackPlan plan(BossReward reward) {
        Objects.requireNonNull(reward, "reward");
        return new RewardStackPlan(
            reward.itemId().equals("sahurs_bat"), reward.itemId(), reward.amount());
    }

    record RewardStackPlan(boolean customItem, String itemId, int amount) { }
}
