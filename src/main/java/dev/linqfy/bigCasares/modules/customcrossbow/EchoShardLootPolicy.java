package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class EchoShardLootPolicy {

    private EchoShardLootPolicy() {
    }

    public static boolean removeAncientCityEchoShards(NamespacedKey lootTableKey, List<ItemStack> loot) {
        if (lootTableKey == null || !"minecraft".equals(lootTableKey.getNamespace())) {
            return false;
        }
        if (!lootTableKey.getKey().startsWith("chests/ancient_city")) {
            return false;
        }

        int previousSize = loot.size();
        loot.removeIf(item -> item != null && item.getType() == Material.ECHO_SHARD);
        return previousSize != loot.size();
    }

    public static ItemStack createWardenEchoShardDrop(int amount) {
        return new ItemStack(Material.ECHO_SHARD, Math.max(1, amount));
    }
}
