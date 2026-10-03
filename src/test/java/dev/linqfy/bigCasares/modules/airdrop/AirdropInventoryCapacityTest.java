package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropInventoryCapacityTest {

    @Test
    void emptyInventoryFitsAllRewards() {
        assertTrue(AirdropInventoryCapacity.canFit(
            new ItemStack[36],
            List.of(new ItemStack(Material.DIAMOND, 16), new ItemStack(Material.GOLD_INGOT, 32)),
            64,
            ignored -> 64
        ));
    }

    @Test
    void partialStackIsUsedBeforeAnEmptySlot() {
        ItemStack[] contents = new ItemStack[2];
        contents[0] = new ItemStack(Material.DIAMOND, 16);

        assertTrue(AirdropInventoryCapacity.canFit(
            contents, List.of(new ItemStack(Material.DIAMOND, 48)), 64, ignored -> 64));
    }

    @Test
    void fullInventoryRejectsRewardsThatCannotFit() {
        ItemStack[] contents = new ItemStack[36];
        for (int index = 0; index < contents.length; index++) {
            contents[index] = new ItemStack(Material.STONE, 64);
        }

        assertFalse(AirdropInventoryCapacity.canFit(
            contents, List.of(new ItemStack(Material.DIAMOND, 1)), 64, ignored -> 64));
    }

    @Test
    void simulationDoesNotMutateThePlayerInventorySnapshot() {
        ItemStack[] contents = {new ItemStack(Material.DIAMOND, 16), null};

        assertTrue(AirdropInventoryCapacity.canFit(
            contents, List.of(new ItemStack(Material.DIAMOND, 32)), 64, ignored -> 64));
        assertTrue(contents[0].getAmount() == 16);
        assertTrue(contents[1] == null);
    }
}
