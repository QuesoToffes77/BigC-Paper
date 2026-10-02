package dev.linqfy.bigCasares.modules.bloodmoon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodMoonModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("blood-moon", new BloodMoonModule(null, () -> false).getId());
    }
}
