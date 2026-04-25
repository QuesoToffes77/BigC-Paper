package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoShardLootPolicyTest {

    @Test
    void removesEchoShardsFromAncientCityLootOnly() {
        List<ItemStack> ancientCityLoot = new ArrayList<>();
        ancientCityLoot.add(new ItemStack(Material.ECHO_SHARD, 3));
        ancientCityLoot.add(new ItemStack(Material.DIAMOND, 1));

        boolean changed = EchoShardLootPolicy.removeAncientCityEchoShards(
            NamespacedKey.minecraft("chests/ancient_city"),
            ancientCityLoot
        );

        assertTrue(changed);
        assertEquals(1, ancientCityLoot.size());
        assertEquals(Material.DIAMOND, ancientCityLoot.getFirst().getType());

        List<ItemStack> otherLoot = new ArrayList<>();
        otherLoot.add(new ItemStack(Material.ECHO_SHARD, 1));
        assertFalse(EchoShardLootPolicy.removeAncientCityEchoShards(
            NamespacedKey.minecraft("chests/buried_treasure"),
            otherLoot
        ));
        assertEquals(1, otherLoot.size());
    }

    @Test
    void wardenDropsEchoShardsWithinConfiguredRange() {
        ItemStack drop = EchoShardLootPolicy.createWardenEchoShardDrop(2);

        assertEquals(Material.ECHO_SHARD, drop.getType());
        assertEquals(2, drop.getAmount());
    }
}
