package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionInteractionTest {
    @Test
    void anvilResultMustActuallyTransfer() {
        assertFalse(MissionListener.takesAnvilResult(InventoryAction.NOTHING));
        assertTrue(MissionListener.takesAnvilResult(InventoryAction.PICKUP_ALL));
        assertTrue(MissionListener.takesAnvilResult(InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }

    @Test
    void waxingRequiresTheExpectedBlockTransition() {
        assertTrue(MissionListener.wasWaxed(Material.COPPER_BLOCK, Material.WAXED_COPPER_BLOCK));
        assertFalse(MissionListener.wasWaxed(Material.COPPER_BLOCK, Material.COPPER_BLOCK));
        assertFalse(MissionListener.wasWaxed(Material.COPPER_ORE, Material.COPPER_ORE));
        assertFalse(MissionListener.wasWaxed(Material.COPPER_BLOCK, Material.WAXED_CUT_COPPER));
    }
}
