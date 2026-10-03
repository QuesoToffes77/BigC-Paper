package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class AirdropFallingTask extends BukkitRunnable {

    private static final double FALL_SPEED = 0.5;

    private static final Set<Material> AIR_MATERIALS = Set.of(
        Material.AIR,
        Material.CAVE_AIR,
        Material.VOID_AIR
    );

    private static final Set<Material> INVALID_GROUNDS = Set.of(
        Material.WATER,
        Material.LAVA,
        Material.VOID_AIR
    );

    private final World world;
    private final Consumer<AirdropPosition> onLand;
    private final Runnable onAbort;
    private final BooleanSupplier active;

    private double currentY;
    private final int targetX;
    private final int targetZ;
    private int groundY;

    public AirdropFallingTask(World world, AirdropPosition position, Consumer<AirdropPosition> onLand) {
        this(world, position, () -> true, onLand, () -> {});
    }

    public AirdropFallingTask(
        World world,
        AirdropPosition position,
        BooleanSupplier active,
        Consumer<AirdropPosition> onLand
    ) {
        this(world, position, active, onLand, () -> {});
    }

    public AirdropFallingTask(
        World world,
        AirdropPosition position,
        BooleanSupplier active,
        Consumer<AirdropPosition> onLand,
        Runnable onAbort
    ) {
        this.world = world;
        this.targetX = position.x();
        this.targetZ = position.z();
        this.groundY = world.getHighestBlockYAt(targetX, targetZ);
        this.currentY = position.y();
        this.active = active;
        this.onLand = onLand;
        this.onAbort = onAbort != null ? onAbort : () -> {};
    }

    @Override
    public void run() {
        if (!active.getAsBoolean()) {
            cancel();
            return;
        }
        currentY -= FALL_SPEED;

        Location particleLoc = new Location(world, targetX + 0.5, currentY, targetZ + 0.5);
        world.spawnParticle(Particle.CLOUD, particleLoc, 5, 0.3, 0.3, 0.3, 0.01);

        int currentGroundY = world.getHighestBlockYAt(targetX, targetZ);
        if (currentY <= currentGroundY + 1) {
            land();
            cancel();
        }
    }

    private void land() {
        // Revalidate ground + space just before landing
        groundY = world.getHighestBlockYAt(targetX, targetZ);
        Block groundBlock = world.getBlockAt(targetX, groundY, targetZ);
        Material groundType = groundBlock.getType();
        if (!groundType.isSolid() || INVALID_GROUNDS.contains(groundType)) {
            onAbort.run();
            return;
        }

        int chestY = groundY + 1;
        while (chestY < world.getMaxHeight() && !isAir(world.getBlockAt(targetX, chestY, targetZ).getType())) {
            chestY++;
        }
        if (chestY >= world.getMaxHeight() || !isAir(world.getBlockAt(targetX, chestY, targetZ).getType())) {
            onAbort.run();
            return;
        }

        Location chestLoc = new Location(world, targetX, chestY, targetZ);
        chestLoc.getBlock().setType(Material.CHEST);
        world.playSound(chestLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.8f);
        onLand.accept(new AirdropPosition(targetX, chestY, targetZ));
    }

    private static boolean isAir(Material material) {
        return AIR_MATERIALS.contains(material);
    }
}
