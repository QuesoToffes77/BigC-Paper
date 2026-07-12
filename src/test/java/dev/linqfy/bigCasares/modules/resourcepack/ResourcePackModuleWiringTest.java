package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourcePackModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("resource-pack-system", new ResourcePackModule(null).getId());
    }
}
