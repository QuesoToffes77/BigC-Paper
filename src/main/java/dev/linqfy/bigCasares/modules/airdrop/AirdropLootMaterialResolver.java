package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AirdropLootMaterialResolver {

    public Map<String, Material> resolveAll() {
        LinkedHashMap<String, Material> resolved = new LinkedHashMap<>();
        for (AirdropType type : AirdropType.values()) {
            for (AirdropQuality quality : AirdropQuality.values()) {
                for (AirdropLootDefinition entry : AirdropLootTable.materials(type, quality)) {
                    Material material = resolved.computeIfAbsent(entry.itemId(), this::resolveOrThrow);
                    resolved.putIfAbsent(material.name(), material);
                }
            }
        }
        resolved.put("enchanted_book", Material.ENCHANTED_BOOK);
        return Map.copyOf(resolved);
    }

    public Material resolveOrThrow(String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalStateException("Invalid airdrop material: " + materialName);
        }
        return material;
    }
}
