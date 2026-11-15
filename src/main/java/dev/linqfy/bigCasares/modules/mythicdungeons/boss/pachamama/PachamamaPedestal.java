package dev.linqfy.bigCasares.modules.mythicdungeons.boss.pachamama;

import dev.linqfy.bigCasares.modules.mythicdungeons.boss.MythicBossSupport;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mandinga's offer: a pedestal block on the floor with his guitar spinning over it. Clicks are read by polling an
 * owned {@link Interaction} entity ({@code getLastInteraction()}), so the encounter needs no listener. Java players
 * see a spinning {@link ItemDisplay}; Bedrock players (Geyser draws no display entities) see a floating dropped item
 * that nobody can pick up, hidden from Java players. Whether Geyser forwards clicks on an Interaction entity depends
 * on its version, so the fight also accepts holding sneak next to the pedestal ({@link PachamamaDealHold}).
 */
final class PachamamaPedestal {

    private static final Material BLOCK = Material.CHISELED_POLISHED_BLACKSTONE;
    private static final float SPIN_PER_TICK = 6.0f;

    private final MythicBossSupport support;
    private final UUID bossId;
    private final PachamamaFloor floor;
    private Block block;
    private Location top;
    private ItemDisplay display;
    private Item bedrockItem;
    private Interaction interaction;
    private long lastSeen = Long.MIN_VALUE;
    private float yaw;

    PachamamaPedestal(MythicBossSupport support, UUID bossId, PachamamaFloor floor) {
        this.support = support;
        this.bossId = bossId;
        this.floor = floor;
    }

    /** Raises the pedestal at the floor block column (x, z), on the walking level {@code floorY}. */
    boolean raise(World world, int x, int floorY, int z, ItemStack guitar) {
        Block target = world.getBlockAt(x, floorY, z);
        if (!target.getType().isAir()) {
            return false;
        }
        block = target;
        floor.change(Map.of(target, BLOCK.createBlockData()));
        top = new Location(world, x + 0.5, floorY + 1.0, z + 0.5);
        Location spin = top.clone().add(0.0, 0.75, 0.0);
        display = support.spawnOwned(bossId, spin, ItemDisplay.class, created -> {
            created.setItemStack(guitar.clone());
            created.setBillboard(Display.Billboard.FIXED);
            created.setPersistent(false);
            created.setTeleportDuration(1);
            created.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                new Vector3f(1.1f, 1.1f, 1.1f), new AxisAngle4f()));
        });
        bedrockItem = world.dropItem(spin, guitar.clone(), item -> {
            item.setCanPlayerPickup(false);
            item.setCanMobPickup(false);
            item.setUnlimitedLifetime(true);
            item.setGravity(false);
            item.setInvulnerable(true);
            item.setPersistent(false);
            item.setVelocity(new Vector());
        });
        support.track(bossId, bedrockItem);
        for (Player online : world.getPlayers()) {
            if (!support.isBedrock(online.getUniqueId())) {
                online.hideEntity(support.plugin(), bedrockItem);
            }
        }
        interaction = support.spawnOwned(bossId, top.clone().subtract(0.0, 1.0, 0.0), Interaction.class, created -> {
            created.setInteractionWidth(1.4f);
            created.setInteractionHeight(2.4f);
            created.setResponsive(true);
            created.setPersistent(false);
        });
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, spin, 40, 0.4, 0.6, 0.4, 0.03);
        world.playSound(top, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.5f, 0.6f);
        return true;
    }

    boolean raised() {
        return top != null;
    }

    Location top() {
        return top == null ? null : top.clone();
    }

    /** Block column of the pedestal. */
    Block block() {
        return block;
    }

    /** The player behind the newest click on the pedestal since the last poll, if online. */
    Optional<Player> poll() {
        if (interaction == null || !interaction.isValid()) {
            return Optional.empty();
        }
        Interaction.PreviousInteraction last = interaction.getLastInteraction();
        if (last == null || last.getTimestamp() == lastSeen) {
            return Optional.empty();
        }
        lastSeen = last.getTimestamp();
        OfflinePlayer who = last.getPlayer();
        return Optional.ofNullable(who.getPlayer());
    }

    /** Spins the guitar, keeps the Bedrock item in place and hidden from Java players; call every tick. */
    void tick(long tick, Collection<? extends Player> viewers) {
        if (top == null) {
            return;
        }
        yaw = (yaw + SPIN_PER_TICK) % 360.0f;
        if (display != null && display.isValid()) {
            display.setRotation(yaw, 0.0f);
        }
        if (bedrockItem != null && bedrockItem.isValid()) {
            Location spin = top.clone().add(0.0, 0.75, 0.0);
            if (bedrockItem.getLocation().distanceSquared(spin) > 0.04) {
                bedrockItem.teleport(spin);
            }
            bedrockItem.setVelocity(new Vector());
            if (tick % 20 == 0) {
                for (Player viewer : viewers) {
                    if (!support.isBedrock(viewer.getUniqueId())) {
                        viewer.hideEntity(support.plugin(), bedrockItem);
                    }
                }
            }
        }
        if (tick % 6 == 0) {
            top.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, top.clone().add(0.0, 0.2, 0.0), 2, 0.35, 0.05,
                0.35, 0.0);
            top.getWorld().spawnParticle(Particle.NOTE, top.clone().add(0.0, 1.4, 0.0), 0,
                (tick % 24) / 24.0, 0.0, 0.0, 1.0);
        }
    }

    /** Removes the entities; the block goes back with the floor journal. */
    void clear() {
        if (display != null && display.isValid()) {
            display.remove();
        }
        if (bedrockItem != null && bedrockItem.isValid()) {
            bedrockItem.remove();
        }
        if (interaction != null && interaction.isValid()) {
            interaction.remove();
        }
        display = null;
        bedrockItem = null;
        interaction = null;
    }
}
