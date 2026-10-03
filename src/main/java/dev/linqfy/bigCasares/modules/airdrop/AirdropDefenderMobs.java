package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * The zombie defense around a landed AirDrop. One burst of vanilla zombies
 * (some geared) plus a few rare horse riders armed with tridents guards the
 * chest with normal mob AI - no per-mob schedulers, no permanent loops. Every
 * entity is tagged and tracked in the current {@link AirdropDefenderWave}, so
 * cleanup only ever removes the mobs this module spawned, never world
 * zombies. Riders keep a zombie-to-horse link: if the rider dies, its horse
 * is removed on the spot, so no ownerless horses are left behind.
 */
final class AirdropDefenderMobs implements Listener, AutoCloseable {

    private static final int MAX_POSITION_ATTEMPTS = 12;
    private static final Set<Material> UNSAFE_GROUND = Set.of(
            Material.WATER,
            Material.LAVA,
            Material.MAGMA_BLOCK,
            Material.CACTUS,
            Material.VOID_AIR
    );

    private final JavaPlugin plugin;
    private final NamespacedKey defenderKey;
    private final NamespacedKey defenderIdKey;
    private final NamespacedKey qualityKey;
    private final NamespacedKey healthModifierKey;
    private final NamespacedKey damageModifierKey;
    private final Random random = new Random();
    private AirdropDefenderWave wave;

    AirdropDefenderMobs(JavaPlugin plugin) {
        this.plugin = plugin;
        this.defenderKey = new NamespacedKey(plugin, "airdrop_defender");
        this.defenderIdKey = new NamespacedKey(plugin, "airdrop_id");
        this.qualityKey = new NamespacedKey(plugin, "airdrop_quality");
        this.healthModifierKey = new NamespacedKey(plugin, "airdrop_quality_health");
        this.damageModifierKey = new NamespacedKey(plugin, "airdrop_quality_damage");
    }

    /**
     * How many zombies and riders to attempt, honoring the hard {@code maxTotal}
     * ceiling. Pure so the cap/chance rules are unit-testable.
     */
    static Plan plan(AirdropMobSettings settings, Random random) {
        if (!settings.enabled() || settings.maxTotal() <= 0) {
            return new Plan(0, 0);
        }
        int zombies = Math.min(settings.zombies().amount(), settings.maxTotal());
        int riders = 0;
        if (settings.horseRiders().enabled() && settings.horseRiders().amount() > 0) {
            for (int index = 0; index < settings.horseRiders().amount(); index++) {
                if (random.nextInt(100) < settings.horseRiders().chancePercent()) {
                    riders++;
                }
            }
        }
        riders = Math.min(riders, Math.max(0, settings.maxTotal() - zombies));
        return new Plan(zombies, riders);
    }

    static Plan plan(AirdropMobSettings settings, AirdropQualityProfile profile, Random random) {
        return plan(settings, AirdropQuality.COMMON, profile, random);
    }

    static Plan plan(
        AirdropMobSettings settings,
        AirdropQuality quality,
        AirdropQualityProfile profile,
        Random random
    ) {
        if (!settings.enabled() || settings.maxTotal() <= 0 || profile.guardCount() <= 0) {
            return new Plan(0, 0);
        }
        int total = Math.min(settings.maxTotal(), profile.guardCount());
        int phantomCreepers = Math.min(
            settings.phantomCreepers().amountFor(quality), total / 2);
        int remaining = total - (phantomCreepers * 2);
        int riderSlots = Math.min(Math.max(0, profile.equipmentLevel() - 1),
            settings.horseRiders().amount());
        int riders = 0;
        if (settings.horseRiders().enabled()) {
            for (int slot = 0; slot < riderSlots; slot++) {
                if (random.nextInt(100) < settings.horseRiders().chancePercent()) {
                    riders++;
                }
            }
        }
        riders = Math.min(riders, remaining / 2);
        return new Plan(remaining - (riders * 2), riders, phantomCreepers);
    }

    /**
     * Spawns the defensive wave around the chest. Any previous wave is cleaned
     * first, so there is never more than one defending group per module
     * instance. Entities that cannot find a valid spot are skipped.
     */
    void spawnAround(World world, AirdropPosition chest, AirdropMobSettings settings) {
        spawnAround(world, chest, settings, AirdropQuality.COMMON,
            AirdropQualitySettings.defaults().profile(AirdropQuality.COMMON), UUID.randomUUID());
    }

    void spawnAround(
        World world,
        AirdropPosition chest,
        AirdropMobSettings settings,
        AirdropQuality quality,
        AirdropQualityProfile profile,
        UUID airdropId
    ) {
        cleanup();
        if (!settings.enabled() || settings.maxTotal() <= 0) {
            return;
        }
        Plan plan = plan(settings, quality, profile, random);
        AirdropDefenderWave newWave = new AirdropDefenderWave();
        int radius = settings.zombies().radius();
        AirdropGuardLoadout loadout = AirdropGuardLoadout.forProfile(profile);

        for (int index = 0; index < plan.zombies(); index++) {
            Location location = findValidSpawn(world, chest, radius, 2);
            if (location == null) {
                break;
            }
            try {
                Zombie zombie = world.spawn(location, Zombie.class, spawned -> {
                    mark(spawned, airdropId, quality);
                    spawned.setPersistent(true);
                    spawned.setRemoveWhenFarAway(false);
                    spawned.setCanPickupItems(false);
                    int gearChance = Math.max(settings.zombies().gearChance(), profile.equipmentLevel() * 20);
                    if (random.nextInt(100) < gearChance) {
                        equipZombie(spawned, loadout, profile.equipmentDropChance(), profile);
                    }
                    AirdropGuardEquipment.applyAttributes(
                        spawned, profile, healthModifierKey, damageModifierKey);
                });
                newWave.track(zombie, location);
            } catch (RuntimeException failure) {
                plugin.getLogger().warning(
                        "[AirDrop] Defender zombie spawn failed: " + failure.getMessage());
            }
        }

        for (int index = 0; index < plan.riders(); index++) {
            Location location = findValidSpawn(world, chest, radius, 3);
            if (location == null) {
                break;
            }
            try {
                spawnRider(world, location, newWave, loadout, profile, airdropId, quality);
            } catch (RuntimeException failure) {
                plugin.getLogger().warning(
                        "[AirDrop] Defender rider spawn failed: " + failure.getMessage());
            }
        }

        for (int index = 0; index < plan.phantomCreepers(); index++) {
            Location location = findValidAirSpawn(
                world, chest, radius, settings.phantomCreepers().spawnHeight());
            if (location == null) {
                break;
            }
            try {
                spawnPhantomCreeper(
                    world, location, newWave, profile, airdropId, quality,
                    settings.phantomCreepers().fireResistanceTicks());
            } catch (RuntimeException failure) {
                plugin.getLogger().warning(
                    "[AirDrop] Phantom-Creeper defender spawn failed: " + failure.getMessage());
            }
        }

        wave = newWave;
    }

    /**
     * Removes every entity of the current wave in loaded chunks. Idempotent; skips dead/missing entities.
     * Chunk loading budget policy: Only process entities in currently loaded chunks.
     * We avoid force-loading chunks via getChunkAt() to prevent lag spikes and respect chunk loading budgets.
     * Any defenders remaining in unloaded chunks are tagged with PersistentDataContainer and will be purged
     * via removeTagged() when chunks are active or during drop restoration.
     */
    void cleanup() {
        if (wave == null) {
            return;
        }
        for (UUID entityId : wave.entityIds()) {
            Location location = wave.positionOf(entityId);
            // Chunk loading budget policy: Only process entities in loaded chunks.
            // Do not force-load chunks via getChunkAt().
            if (location != null && location.getWorld() != null) {
                if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    continue;
                }
            }
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        wave = null;
    }

    /**
     * Removes any entity still tagged as an AirDrop defender (for example the
     * survivors of a wave from before a server restart), so a restored drop
     * never ends up double-guarded.
     */
    void removeTagged(World world) {
        for (Entity entity : new ArrayList<>(world.getEntities())) {
            if (entity.getPersistentDataContainer().has(defenderKey, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }

    @Override
    public void close() {
        cleanup();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (wave == null) {
            return;
        }
        UUID deadId = event.getEntity().getUniqueId();
        if (!wave.contains(deadId)) {
            return;
        }
        UUID companionId = wave.companionOf(deadId);
        if (companionId != null) {
            Entity companion = Bukkit.getEntity(companionId);
            if (companion != null && companion.isValid() && !companion.isDead()) {
                companion.remove();
            }
            wave.untrack(companionId);
        }
        wave.untrack(deadId);
    }

    private void spawnPhantomCreeper(
        World world,
        Location location,
        AirdropDefenderWave wave,
        AirdropQualityProfile profile,
        UUID airdropId,
        AirdropQuality quality,
        int fireResistanceTicks
    ) {
        Phantom phantom = world.spawn(location, Phantom.class, spawned -> {
            mark(spawned, airdropId, quality);
            spawned.setPersistent(true);
            spawned.setRemoveWhenFarAway(false);
            AirdropGuardEquipment.applyAttributes(
                spawned, profile, healthModifierKey, damageModifierKey);
        });
        Creeper creeper = world.spawn(location, Creeper.class, spawned -> {
            mark(spawned, airdropId, quality);
            spawned.setPersistent(true);
            spawned.setRemoveWhenFarAway(false);
            AirdropGuardEquipment.applyAttributes(
                spawned, profile, healthModifierKey, damageModifierKey);
        });

        if (fireResistanceTicks > 0) {
            PotionEffect resistance = new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, fireResistanceTicks, 0, true, false, true);
            phantom.addPotionEffect(resistance);
            creeper.addPotionEffect(resistance);
        }
        if (!phantom.addPassenger(creeper)) {
            phantom.remove();
            creeper.remove();
            throw new IllegalStateException("could not mount Creeper on Phantom");
        }
        wave.trackRider(creeper, phantom);
    }

    private void spawnRider(
        World world,
        Location location,
        AirdropDefenderWave wave,
        AirdropGuardLoadout loadout,
        AirdropQualityProfile profile,
        UUID airdropId,
        AirdropQuality quality
    ) {
        Horse horse = world.spawn(location, Horse.class, spawned -> {
            mark(spawned, airdropId, quality);
            spawned.setPersistent(true);
            spawned.setRemoveWhenFarAway(false);
            spawned.setTamed(false);
        });
        Zombie rider = world.spawn(location, Zombie.class, spawned -> {
            mark(spawned, airdropId, quality);
            spawned.setPersistent(true);
            spawned.setRemoveWhenFarAway(false);
            spawned.setCanPickupItems(false);
            EntityEquipment equipment = spawned.getEquipment();

            if (equipment != null) {
                equipment.setItemInMainHand(new ItemStack(Material.TRIDENT));
                equipment.setHelmet(new ItemStack(loadout.helmet()));
                equipment.setChestplate(new ItemStack(loadout.chestplate()));
                AirdropGuardEquipment.setDropChances(equipment, profile.equipmentDropChance());
            }
            AirdropGuardEquipment.applyAttributes(
                spawned, profile, healthModifierKey, damageModifierKey);
        });
        horse.addPassenger(rider);
        wave.trackRider(rider, horse);
    }

    private void equipZombie(
        Zombie zombie,
        AirdropGuardLoadout loadout,
        double dropChance,
        AirdropQualityProfile profile
    ) {
        AirdropGuardEquipment.equip(
            zombie, loadout, dropChance, healthModifierKey, damageModifierKey,
            profile);
    }

    /**
     * Finds a spawn spot inside the configured radius of the chest that is on
     * solid, safe ground with enough open space above. {@code openSpace} is 2
     * blocks for zombies and 3 for horses. Returns {@code null} after retries.
     */
    private Location findValidSpawn(World world, AirdropPosition chest, int radius, int openSpace) {
        for (int attempt = 0; attempt < MAX_POSITION_ATTEMPTS; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            if (dx == 0 && dz == 0) {
                continue;
            }
            int x = chest.x() + dx;
            int z = chest.z() + dz;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int y = world.getHighestBlockYAt(x, z);
            Material ground = world.getBlockAt(x, y, z).getType();
            if (UNSAFE_GROUND.contains(ground) || !ground.isSolid()) {
                continue;
            }
            boolean open = true;
            for (int dy = 1; dy <= openSpace; dy++) {
                if (!world.getBlockAt(x, y + dy, z).isPassable()) {
                    open = false;
                    break;
                }
            }
            if (!open) {
                continue;
            }
            return new Location(world, x + 0.5, y + 1, z + 0.5);
        }
        return null;
    }

    private Location findValidAirSpawn(
        World world,
        AirdropPosition chest,
        int radius,
        int spawnHeight
    ) {
        for (int attempt = 0; attempt < MAX_POSITION_ATTEMPTS; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int x = chest.x() + dx;
            int z = chest.z() + dz;
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            int surfaceY = world.getHighestBlockYAt(x, z) + 1;
            int y = Math.max(surfaceY + spawnHeight, chest.y() + spawnHeight);
            y = Math.min(y, world.getMaxHeight() - 4);
            if (y <= world.getMinHeight() + 2) {
                continue;
            }
            boolean open = true;
            for (int dy = -1; dy <= 2; dy++) {
                if (!world.getBlockAt(x, y + dy, z).isPassable()) {
                    open = false;
                    break;
                }
            }
            if (open) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    private void mark(Entity entity, UUID airdropId, AirdropQuality quality) {
        entity.getPersistentDataContainer().set(defenderKey, PersistentDataType.BYTE, (byte) 1);
        entity.getPersistentDataContainer().set(defenderIdKey, PersistentDataType.STRING, airdropId.toString());
        entity.getPersistentDataContainer().set(qualityKey, PersistentDataType.STRING, quality.name());
    }

    record Plan(int zombies, int riders, int phantomCreepers) {
        Plan(int zombies, int riders) {
            this(zombies, riders, 0);
        }

        int totalEntities() {
            return zombies + (riders * 2) + (phantomCreepers * 2);
        }
    }
}
