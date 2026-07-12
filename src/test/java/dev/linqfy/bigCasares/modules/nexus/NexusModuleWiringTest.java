package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusModuleWiringTest {

    @Test
    void moduleIdIsStable() {
        assertEquals("nexus-system", new NexusModule(null).getId());
    }
}
