package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;

record AirdropGuardLoadout(
    Material weapon,
    Material helmet,
    Material chestplate,
    Material leggings,
    Material boots,
    int enchantmentLevel,
    int equipmentScore
) {
    static AirdropGuardLoadout forProfile(AirdropQualityProfile profile) {
        return switch (profile.equipmentLevel()) {
            case 1 -> new AirdropGuardLoadout(Material.STONE_SWORD, Material.LEATHER_HELMET,
                Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, 0, 1);
            case 2 -> new AirdropGuardLoadout(Material.IRON_SWORD, Material.IRON_HELMET,
                Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, 1, 2);
            case 3 -> new AirdropGuardLoadout(Material.DIAMOND_SWORD, Material.DIAMOND_HELMET,
                Material.IRON_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.IRON_BOOTS, 2, 3);
            case 4 -> new AirdropGuardLoadout(Material.DIAMOND_SWORD, Material.DIAMOND_HELMET,
                Material.NETHERITE_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, 3, 4);
            case 5 -> new AirdropGuardLoadout(Material.NETHERITE_SWORD, Material.NETHERITE_HELMET,
                Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS, 4, 5);
            default -> throw new IllegalArgumentException("unsupported equipment level");
        };
    }
}
