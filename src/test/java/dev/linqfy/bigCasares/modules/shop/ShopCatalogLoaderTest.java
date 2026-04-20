package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopCatalogLoaderTest {

    @Test
    void loadsCategoryAndBothItemSources() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories.blocks.name", "&6Bloques");
        config.set("categories.blocks.icon", "STONE");
        config.set("categories.blocks.slot", 10);
        config.set("categories.blocks.items.stone.slot", 10);
        config.set("categories.blocks.items.stone.material", "STONE");
        config.set("categories.blocks.items.stone.amount", 64);
        config.set("categories.blocks.items.stone.buy-price", 64.0);
        config.set("categories.blocks.items.stone.sell-price", 4.0);
        config.set("categories.blocks.items.apple.slot", 12);
        config.set("categories.blocks.items.apple.custom-item-id", "copper_apple");
        config.set("categories.blocks.items.apple.amount", 1);
        config.set("categories.blocks.items.apple.buy-price", 1500.0);
        config.set("categories.blocks.items.apple.sell-price", 50.0);

        ShopCatalog catalog = new ShopCatalogLoader().load(config);

        assertEquals(1, catalog.categories().size());
        ShopCategory category = catalog.category("blocks").orElseThrow();
        assertEquals("&6Bloques", category.name());
        assertEquals(Material.STONE, category.icon());
        assertEquals(2, category.entries().size());
        assertEquals(Material.STONE, category.entries().getFirst().material());
        assertEquals("copper_apple", category.entries().get(1).customItemId());
    }

    @Test
    void rejectsEntryWithBothItemSources() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("categories.blocks.name", "&6Bloques");
        config.set("categories.blocks.icon", "STONE");
        config.set("categories.blocks.slot", 10);
        config.set("categories.blocks.items.invalid.slot", 10);
        config.set("categories.blocks.items.invalid.material", "STONE");
        config.set("categories.blocks.items.invalid.custom-item-id", "copper_apple");
        config.set("categories.blocks.items.invalid.amount", 1);
        config.set("categories.blocks.items.invalid.buy-price", 10.0);
        config.set("categories.blocks.items.invalid.sell-price", 1.0);

        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new ShopCatalogLoader().load(config)
        );

        assertEquals("La entrada invalid debe definir exactamente una fuente de item.", thrown.getMessage());
    }
}
