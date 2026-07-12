package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShopCatalogEntryResolutionTest {

    @Test
    void resolvesTheEntryFromTheCurrentCatalog() {
        ShopEntry currentEntry = new ShopEntry(
            "stone", 10, Material.STONE, null, 1, 250.0, 0.0, "Piedra", List.of());
        ShopCatalog currentCatalog = new ShopCatalog(List.of(
            new ShopCategory("blocks", "Bloques", Material.STONE, 0, List.of(currentEntry))));

        ShopEntry resolved = currentCatalog.entry("blocks", "stone").orElseThrow();

        assertSame(currentEntry, resolved);
        assertEquals(250.0, resolved.buyPrice());
    }
}
