package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcidRainModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("acid-rain", new AcidRainModule(null).getId());
    }
}
