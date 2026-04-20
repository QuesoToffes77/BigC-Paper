package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.Material;

import java.util.List;

public record ShopCategory(
    String id,
    String name,
    Material icon,
    int slot,
    List<ShopEntry> entries
) {
}
