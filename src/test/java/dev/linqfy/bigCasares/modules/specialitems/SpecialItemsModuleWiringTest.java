package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialItemsModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("special-items", new SpecialItemsModule(null).getId());
    }
}
