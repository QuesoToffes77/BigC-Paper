package dev.linqfy.bigCasares.modules.airdrop;

import java.util.List;
import java.util.EnumMap;
import java.util.Map;

public final class AirdropLootTable {

    private static final Map<AirdropType, Map<AirdropQuality, List<AirdropLootDefinition>>> MATERIAL_POOLS
        = createMaterialPools();
    private static final Map<AirdropQuality, List<AirdropLootDefinition>> CUSTOM_POOLS
        = createCustomPools();

    private AirdropLootTable() {}

    public static List<AirdropLootDefinition> materials(AirdropType type, AirdropQuality quality) {
        Map<AirdropQuality, List<AirdropLootDefinition>> pools = MATERIAL_POOLS.get(type);
        return pools == null ? List.of() : pools.getOrDefault(quality, List.of());
    }

    public static List<AirdropLootDefinition> customItems(AirdropQuality quality) {
        return CUSTOM_POOLS.getOrDefault(quality, List.of());
    }

    private static Map<AirdropType, Map<AirdropQuality, List<AirdropLootDefinition>>> createMaterialPools() {
        EnumMap<AirdropType, Map<AirdropQuality, List<AirdropLootDefinition>>> types
            = new EnumMap<>(AirdropType.class);

        EnumMap<AirdropQuality, List<AirdropLootDefinition>> combat = new EnumMap<>(AirdropQuality.class);
        combat.put(AirdropQuality.COMMON, List.of(
            material("ARROW", 12, 32, 5), material("GUNPOWDER", 6, 16, 4),
            material("COOKED_BEEF", 4, 10, 3), material("TNT", 2, 5, 2)));
        combat.put(AirdropQuality.RARE, List.of(
            material("ARROW", 24, 48, 4), material("GUNPOWDER", 16, 32, 4),
            material("TNT", 5, 10, 3), material("FIRE_CHARGE", 6, 16, 2),
            material("IRON_SWORD", 1, 1, 1)));
        combat.put(AirdropQuality.EPIC, List.of(
            material("TNT", 10, 20, 4), material("GUNPOWDER", 24, 48, 3),
            material("FIREWORK_ROCKET", 12, 24, 2), material("DIAMOND_SWORD", 1, 1, 1),
            material("GOLDEN_APPLE", 1, 3, 1)));
        combat.put(AirdropQuality.LEGENDARY, List.of(
            material("TNT", 18, 32, 4), material("GUNPOWDER", 40, 64, 3),
            material("DIAMOND_SWORD", 1, 1, 2), material("TOTEM_OF_UNDYING", 1, 1, 1),
            material("NETHERITE_SCRAP", 1, 2, 1)));
        combat.put(AirdropQuality.GHISTIC, List.of(
            material("TNT", 32, 64, 4), material("GUNPOWDER", 64, 96, 3),
            material("NETHERITE_SWORD", 1, 1, 1), material("TOTEM_OF_UNDYING", 1, 2, 1),
            material("ENCHANTED_GOLDEN_APPLE", 1, 1, 1)));
        types.put(AirdropType.HE, Map.copyOf(combat));

        EnumMap<AirdropQuality, List<AirdropLootDefinition>> luxury = new EnumMap<>(AirdropQuality.class);
        luxury.put(AirdropQuality.COMMON, List.of(
            material("IRON_INGOT", 3, 8, 5), material("COAL", 8, 20, 4),
            material("REDSTONE", 8, 20, 4), material("GOLD_INGOT", 1, 4, 2)));
        luxury.put(AirdropQuality.RARE, List.of(
            material("IRON_INGOT", 8, 16, 5), material("GOLD_INGOT", 4, 10, 4),
            material("LAPIS_LAZULI", 8, 24, 3), material("EMERALD", 2, 6, 3),
            material("DIAMOND", 1, 2, 1)));
        luxury.put(AirdropQuality.EPIC, List.of(
            material("DIAMOND", 2, 6, 4), material("EMERALD", 4, 12, 4),
            material("GOLD_INGOT", 8, 16, 3), material("GOLDEN_APPLE", 1, 3, 1)));
        luxury.put(AirdropQuality.LEGENDARY, List.of(
            material("DIAMOND", 5, 12, 5), material("EMERALD", 10, 24, 3),
            material("NETHERITE_SCRAP", 1, 2, 2), material("ANCIENT_DEBRIS", 1, 2, 1),
            material("ENCHANTED_GOLDEN_APPLE", 1, 1, 1)));
        luxury.put(AirdropQuality.GHISTIC, List.of(
            material("DIAMOND", 8, 20, 5), material("NETHERITE_SCRAP", 2, 5, 3),
            material("ANCIENT_DEBRIS", 2, 4, 2), material("NETHERITE_INGOT", 1, 2, 1),
            material("ENCHANTED_GOLDEN_APPLE", 1, 1, 1)));
        types.put(AirdropType.LUXURY, Map.copyOf(luxury));

        EnumMap<AirdropQuality, List<AirdropLootDefinition>> rareFallback = new EnumMap<>(AirdropQuality.class);
        rareFallback.put(AirdropQuality.COMMON, List.of(
            material("COPPER_INGOT", 8, 20, 5), material("IRON_INGOT", 4, 10, 4),
            material("EXPERIENCE_BOTTLE", 4, 10, 2)));
        rareFallback.put(AirdropQuality.RARE, List.of(
            material("EMERALD", 3, 8, 4), material("DIAMOND", 1, 3, 2),
            material("EXPERIENCE_BOTTLE", 8, 18, 3)));
        rareFallback.put(AirdropQuality.EPIC, List.of(
            material("DIAMOND", 3, 7, 4), material("EMERALD", 8, 16, 3),
            material("GOLDEN_APPLE", 1, 3, 2)));
        rareFallback.put(AirdropQuality.LEGENDARY, List.of(
            material("DIAMOND", 6, 12, 4), material("NETHERITE_SCRAP", 1, 2, 2),
            material("TOTEM_OF_UNDYING", 1, 1, 1)));
        rareFallback.put(AirdropQuality.GHISTIC, List.of(
            material("DIAMOND", 10, 20, 4), material("NETHERITE_SCRAP", 2, 4, 2),
            material("ENCHANTED_GOLDEN_APPLE", 1, 1, 1)));
        types.put(AirdropType.RARE_ITEMS, Map.copyOf(rareFallback));
        return Map.copyOf(types);
    }

    private static Map<AirdropQuality, List<AirdropLootDefinition>> createCustomPools() {
        EnumMap<AirdropQuality, List<AirdropLootDefinition>> pools = new EnumMap<>(AirdropQuality.class);
        pools.put(AirdropQuality.COMMON, List.of(
            custom("copper_apple", 5), custom("nitric_acid", 4), custom("potassium_nitrate", 3)));
        pools.put(AirdropQuality.RARE, List.of(
            custom("grappling_hook_1", 4), custom("glider_tier_1", 4),
            custom("copper_apple", 3), custom("nitric_acid", 3)));
        pools.put(AirdropQuality.EPIC, List.of(
            custom("grappling_hook_2", 4), custom("grappling_hook_3", 2),
            custom("glider_tier_2", 4), custom("glider_tier_3", 2), custom("smoke_bomb", 2)));
        pools.put(AirdropQuality.LEGENDARY, List.of(
            custom("grappling_hook_4", 4), custom("grappling_hook_5", 2),
            custom("glider_tier_4", 4), custom("glider_tier_5", 2), custom("tracker_compass", 1)));
        pools.put(AirdropQuality.GHISTIC, List.of(
            custom("grappling_hook_6", 4), custom("glider_tier_6", 4),
            custom("nuke_shot", 1), custom("sahurs_bat", 1)));
        return Map.copyOf(pools);
    }

    private static AirdropLootDefinition material(String id, int min, int max, int weight) {
        return AirdropLootDefinition.material(id, min, max, weight);
    }

    private static AirdropLootDefinition custom(String id, int weight) {
        return AirdropLootDefinition.custom(id, weight);
    }
}
