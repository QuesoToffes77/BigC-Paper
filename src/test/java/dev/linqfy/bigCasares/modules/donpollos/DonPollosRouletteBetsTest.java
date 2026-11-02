package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosRouletteBetsTest {

    @Test
    void plainItemsCanBeBet() {
        assertTrue(DonPollosRouletteBets.allowed("STONE", false));
        assertTrue(DonPollosRouletteBets.allowed("DIAMOND", false));
    }

    @Test
    void anythingWithItemMetaIsRejected() {
        // encantado, con nombre, libro escrito, pocion o cofre con contenido: todos tienen item meta
        assertFalse(DonPollosRouletteBets.allowed("DIAMOND_SWORD", true));
        assertFalse(DonPollosRouletteBets.allowed("PAPER", true));
        assertFalse(DonPollosRouletteBets.allowed("WRITTEN_BOOK", true));
        assertFalse(DonPollosRouletteBets.allowed("POTION", true));
        assertFalse(DonPollosRouletteBets.allowed("CHEST", true));
    }

    @Test
    void shulkerBoxesAndBundlesAreRejectedEvenWhenEmpty() {
        assertFalse(DonPollosRouletteBets.allowed("SHULKER_BOX", false));
        assertFalse(DonPollosRouletteBets.allowed("RED_SHULKER_BOX", false));
        assertFalse(DonPollosRouletteBets.allowed("light_blue_shulker_box", false));
        assertFalse(DonPollosRouletteBets.allowed("BUNDLE", false));
        assertFalse(DonPollosRouletteBets.allowed("BLUE_BUNDLE", false));
    }
}
