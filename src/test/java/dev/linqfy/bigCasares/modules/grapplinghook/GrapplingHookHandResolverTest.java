package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapplingHookHandResolverTest {

    @Test
    void resolvesTheActualEventHand() {
        assertEquals(GrapplingHookTier.III, GrapplingHookHandResolver.select(
            "grappling_hook_3", "grappling_hook_6", EquipmentSlot.HAND).orElseThrow().tier());
        assertEquals(GrapplingHookTier.VI, GrapplingHookHandResolver.select(
            "grappling_hook_3", "grappling_hook_6", EquipmentSlot.OFF_HAND).orElseThrow().tier());
    }

    @Test
    void ambiguousInputsPreferMainHandDeterministically() {
        assertEquals(EquipmentSlot.HAND, GrapplingHookHandResolver.select(
            "grappling_hook_2", "grappling_hook_6", null).orElseThrow().hand());
    }

    @Test
    void vanillaFishingRodIsNeverResolvedAsCustomHook() {
        assertTrue(GrapplingHookHandResolver.select("fishing_rod", null, EquipmentSlot.HAND).isEmpty());
    }
}
