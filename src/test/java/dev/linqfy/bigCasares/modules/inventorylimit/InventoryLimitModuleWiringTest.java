package dev.linqfy.bigCasares.modules.inventorylimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryLimitModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("inventory-limit", new InventoryLimitModule(null).getId());
    }
}
