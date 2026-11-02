package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosSecretOfferingTest {

    @Test
    void theGorditoAsksForSalsaPicanteAStackOfChickenAndFiveDiamonds() {
        assertEquals(1, DonPollosSecretOffering.SALSA.amount);
        assertEquals(DonPollosProduct.SALSA, DonPollosSecretOffering.SALSA.product);
        assertEquals(1, DonPollosSecretOffering.PICANTE.amount);
        assertEquals(64, DonPollosSecretOffering.POLLO_FRITO.amount);
        assertEquals(DonPollosProduct.POLLO_FRITO, DonPollosSecretOffering.POLLO_FRITO.product);
        assertEquals(5, DonPollosSecretOffering.DIAMANTES.amount);
        assertEquals("DIAMOND", DonPollosSecretOffering.DIAMANTES.material);
        assertEquals("64 Pollo Frito", DonPollosSecretOffering.POLLO_FRITO.plain());
    }

    @Test
    void theSecretNeedsEverything() {
        assertFalse(DonPollosSecretOffering.ready(EnumSet.of(DonPollosSecretOffering.SALSA, DonPollosSecretOffering.PICANTE)));
        assertFalse(DonPollosSecretOffering.ready(EnumSet.complementOf(EnumSet.of(DonPollosSecretOffering.DIAMANTES))));
        assertTrue(DonPollosSecretOffering.ready(EnumSet.allOf(DonPollosSecretOffering.class)));
    }
}
