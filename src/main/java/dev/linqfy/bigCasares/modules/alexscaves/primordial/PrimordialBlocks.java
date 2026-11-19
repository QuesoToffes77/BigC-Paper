package dev.linqfy.bigCasares.modules.alexscaves.primordial;

import dev.linqfy.bigCasares.modules.alexscaves.core.block.ModBlockPalette;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashSet;
import java.util.Set;

/**
 * The module blocks the Primordial boss deals with, and the mod's block tags about them
 * ({@code regenerates_after_primordial_boss_fight}, {@code volcano_blocks}, {@code luxtructosaurus_breaks}).
 */
public final class PrimordialBlocks {
    final BlockData primalMagma;
    /** Mod {@code ACTIVE=true, PERMANENT=true}: the volcano's own magma. */
    final BlockData permanentMagma;
    /** Mod {@code ACTIVE=true, PERMANENT=false}: magma a boss fight lights up. */
    final BlockData bossMagma;
    final BlockData volcanicCore;
    final BlockData limestone;
    final BlockData floodBasalt;
    final BlockData[] fissure = new BlockData[5];
    final BlockData amberMonolith;
    private final Set<BlockData> regenerates = new HashSet<>();
    private final Set<BlockData> volcano = new HashSet<>();
    private final Set<BlockData> breaks = new HashSet<>();
    private final Set<BlockData> nibbles = new HashSet<>();

    public PrimordialBlocks(ModBlockPalette palette) {
        primalMagma = palette.get("primal_magma");
        permanentMagma = palette.get("primal_magma", "active");
        bossMagma = palette.get("primal_magma", "boss");
        volcanicCore = palette.get("volcanic_core");
        limestone = palette.get("limestone");
        floodBasalt = palette.get("flood_basalt", "y");
        amberMonolith = palette.get("amber_monolith");
        for (int i = 0; i < fissure.length; i++) {
            fissure[i] = palette.get("fissure_primal_magma", "r" + i);
        }
        regenerates.add(limestone);
        regenerates.add(primalMagma);
        regenerates.add(permanentMagma);
        regenerates.add(bossMagma);
        volcano.add(volcanicCore);
        volcano.add(primalMagma);
        volcano.add(permanentMagma);
        volcano.add(bossMagma);
        for (String axis : new String[] {"x", "y", "z"}) {
            regenerates.add(palette.get("flood_basalt", axis));
            volcano.add(palette.get("flood_basalt", axis));
        }
        // Mod #minecraft:logs also lists the module's own logs (pewen and thornwood, stripped or not).
        for (String id : new String[] {"pewen_log", "pewen_wood", "stripped_pewen_log", "stripped_pewen_wood", "thornwood_log",
            "thornwood_wood", "stripped_thornwood_log", "stripped_thornwood_wood", "pewen_branch"}) {
            if (palette.registry().blockIds().contains(id)) {
                for (String variant : palette.registry().variants(id)) {
                    breaks.add(palette.get(id, variant));
                }
            }
        }
        // Mod tag relicheirus_knockable_leaves: the module's own leafy blocks (the vanilla ones are checked by the caller).
        for (String id : new String[] {"pewen_branch", "pewen_pines", "tree_star", "thornwood_branch", "archaic_vine"}) {
            if (palette.registry().blockIds().contains(id)) {
                for (String variant : palette.registry().variants(id)) {
                    nibbles.add(palette.get(id, variant));
                }
            }
        }
        for (BlockData data : fissure) {
            volcano.add(data);
        }
    }

    /** Index of a fissure's regeneration height, or -1 when it is not a fissure. */
    int fissureRegen(BlockData data) {
        for (int i = 0; i < fissure.length; i++) {
            if (fissure[i].equals(data)) {
                return i;
            }
        }
        return -1;
    }

    boolean isFissure(BlockData data) {
        return fissureRegen(data) >= 0;
    }

    /** Any of the magma blocks that burn what touches them: the volcano's, a boss fight's and the fissures. */
    boolean burns(BlockData data) {
        return permanentMagma.equals(data) || bossMagma.equals(data) || isFissure(data);
    }

    /** Mod tag {@code regenerates_after_primordial_boss_fight}. */
    boolean regenerates(Block block) {
        Material type = block.getType();
        if (Tag.BASE_STONE_OVERWORLD.isTagged(type) || Tag.DIRT.isTagged(type) || Tag.SAND.isTagged(type)
            || type == Material.SANDSTONE || type == Material.PACKED_MUD) {
            return true;
        }
        return type == Material.NOTE_BLOCK && regenerates.contains(block.getBlockData());
    }

    /** Mod {@code VolcanicCoreBlockEntity.spawnTephra}: it erupts when the top is the core, primal magma, a fissure or lava. */
    boolean erupts(Block top) {
        Material type = top.getType();
        if (type == Material.LAVA) {
            return true;
        }
        BlockData data = top.getBlockData();
        return type == Material.NOTE_BLOCK && (volcanicCore.equals(data) || primalMagma.equals(data)
            || permanentMagma.equals(data) || bossMagma.equals(data) || isFissure(data));
    }

    /** Mod tag {@code volcano_blocks}. */
    boolean volcano(Block block) {
        Material type = block.getType();
        return type == Material.LAVA || type == Material.NOTE_BLOCK && volcano.contains(block.getBlockData());
    }

    /** Mod tag {@code luxtructosaurus_breaks}: leaves, logs and pewen wood. */
    boolean breaks(Block block) {
        Material type = block.getType();
        if (Tag.LEAVES.isTagged(type) || Tag.LOGS.isTagged(type)) {
            return true;
        }
        return type == Material.NOTE_BLOCK && breaks.contains(block.getBlockData());
    }

    /** Mod tags {@code relicheirus_knockable_leaves} and the archaic vine: the module blocks the Atlatitan nibbles. */
    boolean nibbles(Block block) {
        return block.getType() == Material.NOTE_BLOCK && nibbles.contains(block.getBlockData());
    }

    /** Mod tag {@code unmovable}: wither-immune blocks. */
    static boolean unmovable(Material type) {
        return Tag.WITHER_IMMUNE.isTagged(type);
    }

    static BlockData vanilla(Material material) {
        return Bukkit.createBlockData(material);
    }
}
