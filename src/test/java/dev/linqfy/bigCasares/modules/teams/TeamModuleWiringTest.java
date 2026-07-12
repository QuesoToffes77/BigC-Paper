package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamModuleWiringTest {

    @Test
    void exposesStableIdEvenBeforeRuntimeWiringExists() {
        TeamModule module = new TeamModule(null);

        assertEquals("team-system", module.getId());
        assertTrue(module.commandService().isEmpty());
    }
}
