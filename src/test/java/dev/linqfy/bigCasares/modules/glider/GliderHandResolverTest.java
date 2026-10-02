package dev.linqfy.bigCasares.modules.glider;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GliderHandResolverTest {

    @Test
    void gliderWorksFromMainHandAndOffHand() {
        assertEquals(EquipmentSlot.HAND,
            GliderHandResolver.select("glider_tier_3", null).orElseThrow().hand());
        assertEquals(EquipmentSlot.OFF_HAND,
            GliderHandResolver.select(null, "glider_tier_3").orElseThrow().hand());
    }

    @Test
    void dualHandSelectionUsesOneHighestTierSession() {
        GliderHandResolver.Selection selected = GliderHandResolver.select(
            "glider_tier_3", "glider_tier_5").orElseThrow();

        assertEquals(EquipmentSlot.OFF_HAND, selected.hand());
        assertEquals(GliderTier.V, selected.tier());
    }

    @Test
    void equalTiersPreferMainHandAndRemovingBothReturnsEmpty() {
        assertEquals(EquipmentSlot.HAND,
            GliderHandResolver.select("glider_tier_4", "glider_tier_4").orElseThrow().hand());
        assertTrue(GliderHandResolver.select(null, null).isEmpty());
    }
}
