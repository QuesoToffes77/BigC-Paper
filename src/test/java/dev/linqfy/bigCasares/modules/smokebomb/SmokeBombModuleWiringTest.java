package dev.linqfy.bigCasares.modules.smokebomb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeBombModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("smoke-bomb", new SmokeBombModule(null).getId());
    }
}
