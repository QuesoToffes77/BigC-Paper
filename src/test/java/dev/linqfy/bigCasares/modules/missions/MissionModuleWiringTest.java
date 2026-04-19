package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("mission-system", new MissionModule(null).getId());
    }
}
