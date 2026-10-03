package dev.linqfy.bigCasares.modules.acidrain;

import dev.linqfy.bigCasares.BigCasares;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

final class BukkitAcidRainBlockAccess implements AcidRainBlockAccess {
    private final BigCasares plugin;
    private final AcidRainSnapshot storm;

    BukkitAcidRainBlockAccess(BigCasares plugin, AcidRainSnapshot storm) {
        this.plugin = plugin;
        this.storm = storm;
    }

    @Override
    public Material materialAt(AcidRainErosionCandidate candidate) {
        Block block = block(candidate);
        return block == null ? Material.AIR : block.getType();
    }

    @Override
    public boolean canBreak(AcidRainErosionCandidate candidate) {
        Block block = block(candidate);
        if (block == null || !storm.worlds().isAffected(block.getWorld().getName())) {
            return false;
        }
        if (plugin.getNexusModule() != null && plugin.getNexusModule().isProtectedContainer(block)) {
            return false;
        }
        AcidRainBlockErodeEvent event = new AcidRainBlockErodeEvent(block, storm.level());
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    @Override
    public void breakBlock(AcidRainErosionCandidate candidate) {
        Block block = block(candidate);
        if (block != null) {
            block.setType(Material.AIR, false);
        }
    }

    private Block block(AcidRainErosionCandidate candidate) {
        World world = plugin.getServer().getWorld(candidate.worldName());
        if (world == null) {
            return null;
        }
        return world.getBlockAt(candidate.x(), candidate.y(), candidate.z());
    }
}
