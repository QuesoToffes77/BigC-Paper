package dev.linqfy.bigCasares.modules.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopFormGenerationTest {

    @Test
    void rejectsResponsesFromAFormOpenedBeforeReload() {
        ShopFormGeneration generations = new ShopFormGeneration();
        long oldGeneration = generations.activateNext();

        long currentGeneration = generations.activateNext();

        assertNotEquals(oldGeneration, currentGeneration);
        assertFalse(generations.accepts(oldGeneration, true));
        assertTrue(generations.accepts(currentGeneration, true));
    }

    @Test
    void rejectsResponsesWhenPlayerIsOfflineOrModuleIsDisabled() {
        ShopFormGeneration generations = new ShopFormGeneration();
        long generation = generations.activateNext();

        assertFalse(generations.accepts(generation, false));

        generations.invalidate();

        assertFalse(generations.accepts(generation, true));
    }
}
