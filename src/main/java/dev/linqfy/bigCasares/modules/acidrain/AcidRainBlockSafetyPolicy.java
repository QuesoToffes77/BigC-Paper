package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Material;

import java.util.Set;

public final class AcidRainBlockSafetyPolicy {
    private static final Set<Material> CROPS = Set.of(
        Material.WHEAT,
        Material.CARROTS,
        Material.POTATOES,
        Material.BEETROOTS,
        Material.NETHER_WART,
        Material.MELON_STEM,
        Material.ATTACHED_MELON_STEM,
        Material.PUMPKIN_STEM,
        Material.ATTACHED_PUMPKIN_STEM,
        Material.COCOA,
        Material.SWEET_BERRY_BUSH,
        Material.TORCHFLOWER_CROP,
        Material.PITCHER_CROP
    );
    private static final Set<Material> SPECIAL_PROTECTED = Set.of(
        Material.BEDROCK,
        Material.END_PORTAL,
        Material.END_PORTAL_FRAME,
        Material.NETHER_PORTAL,
        Material.SPAWNER,
        Material.COMMAND_BLOCK,
        Material.CHAIN_COMMAND_BLOCK,
        Material.REPEATING_COMMAND_BLOCK,
        Material.STRUCTURE_BLOCK,
        Material.JIGSAW,
        Material.BARRIER,
        Material.LIGHT
    );
    private static final Set<Material> CONTAINERS = Set.of(
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.BARREL,
        Material.SHULKER_BOX,
        Material.ENDER_CHEST,
        Material.HOPPER,
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.DISPENSER,
        Material.DROPPER,
        Material.BREWING_STAND,
        Material.CRAFTER,
        Material.CHISELED_BOOKSHELF,
        Material.DECORATED_POT
    );

    private final AcidRainEnvironmentSettings settings;

    private AcidRainBlockSafetyPolicy(AcidRainEnvironmentSettings settings) {
        this.settings = settings;
    }

    public static AcidRainBlockSafetyPolicy fromSettings(AcidRainEnvironmentSettings settings) {
        return new AcidRainBlockSafetyPolicy(settings);
    }

    public boolean canErode(Material material) {
        AcidRainDestructionSettings destruction = settings.destruction();
        if (material == null || isAir(material) || !destruction.canDestroyBlocks()) {
            return false;
        }
        if (!destruction.whitelist().contains(material)) {
            return false;
        }
        if (isLeaf(material) || isOre(material) || isContainer(material) || SPECIAL_PROTECTED.contains(material)) {
            return false;
        }
        if (!destruction.cropsEnabled() && CROPS.contains(material)) {
            return false;
        }
        return true;
    }

    private static boolean isLeaf(Material material) {
        return material.name().endsWith("_LEAVES");
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    private static boolean isContainer(Material material) {
        return CONTAINERS.contains(material) || material.name().endsWith("_SHULKER_BOX");
    }
}
