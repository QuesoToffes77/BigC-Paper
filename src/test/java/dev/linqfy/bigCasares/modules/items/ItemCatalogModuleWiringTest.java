package dev.linqfy.bigCasares.modules.items;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemCatalogModuleWiringTest {

    @Test
    void moduleIdIsStable() {
        assertEquals("custom-item-catalog", new ItemCatalogModule(null, new CustomItemRegistry()).getId());
    }
}
