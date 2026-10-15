package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosRouletteBetsTest {

    @Test
    void plainItemsCanBeBet() {
        assertTrue(DonPollosRouletteBets.allowed("DIAMOND", false, false));
        assertTrue(DonPollosRouletteBets.allowed("ENCHANTED_GOLDEN_APPLE", false, false));
    }

    @Test
    void shulkerBoxesOfAnyColorAreRejected() {
        assertFalse(DonPollosRouletteBets.allowed("SHULKER_BOX", false, false));
        assertFalse(DonPollosRouletteBets.allowed("RED_SHULKER_BOX", false, false));
        assertFalse(DonPollosRouletteBets.allowed("light_blue_shulker_box", false, false));
    }

    @Test
    void bundlesAreRejected() {
        assertFalse(DonPollosRouletteBets.allowed("BUNDLE", false, false));
        assertFalse(DonPollosRouletteBets.allowed("BLUE_BUNDLE", false, false));
    }

    @Test
    void itemsWithPluginDataOrCustomNameAreRejected() {
        assertFalse(DonPollosRouletteBets.allowed("DIAMOND", true, false));
        assertFalse(DonPollosRouletteBets.allowed("DIAMOND", false, true));
    }
}
