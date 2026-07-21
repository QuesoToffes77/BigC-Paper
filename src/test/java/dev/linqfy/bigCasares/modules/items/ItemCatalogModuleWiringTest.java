package dev.linqfy.bigCasares.modules.items;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCatalogModuleWiringTest {

    @Test
    void moduleIdIsStable() {
        assertEquals("custom-item-catalog", new ItemCatalogModule(null, new CustomItemRegistry()).getId());
    }

    @Test
    void seedsEveryCustomArrowDefinitionUsedByTheCrossbowModule() throws Exception {
        Field field = ItemCatalogModule.class.getDeclaredField("DEFAULT_FILES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> files = (List<String>) field.get(null);

        assertTrue(files.contains("echo_arrow.yml"));
        assertTrue(files.contains("golden_tipped_amethyst_arrow.yml"));
    }
}
