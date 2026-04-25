package dev.linqfy.bigCasares.modules.customcrossbow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomCrossbowModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("custom-crossbow", new CustomCrossbowModule(null).getId());
    }
}
