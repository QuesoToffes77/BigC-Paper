package dev.linqfy.bigCasares.modules.servercontrol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ServerControlModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("server-control-system", new ServerControlModule(null, null, null).getId());
    }
}
