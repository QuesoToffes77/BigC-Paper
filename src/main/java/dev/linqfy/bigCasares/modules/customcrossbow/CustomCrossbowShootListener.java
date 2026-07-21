package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.Location;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

public final class CustomCrossbowShootListener implements Listener {

    private final CustomCrossbowData crossbowData;
    private final PrismarineArrowItem prismarineArrowItem;
    private final EchoArrowItem echoArrowItem;
    private final GoldenTippedAmethystArrowItem amethystArrowItem;
    private final CustomCrossbowSettings settings;
    private final CustomCrossbowDurabilityService durabilityService;
    private final EchoShardCooldownService echoShardCooldownService;
    private final BigCasares plugin;
    private final Consumer<AbstractArrow> prismarineArrowTracker;
    private final Consumer<Player> rocketJumpTracker;
    private final BukkitRuntimeRegistrations registrations;

    public CustomCrossbowShootListener(
        BigCasares plugin,
        CustomCrossbowData crossbowData,
        PrismarineArrowItem prismarineArrowItem,
        EchoArrowItem echoArrowItem,
        GoldenTippedAmethystArrowItem amethystArrowItem,
        CustomCrossbowSettings settings,
        CustomCrossbowDurabilityService durabilityService,
        EchoShardCooldownService echoShardCooldownService,
        Consumer<AbstractArrow> prismarineArrowTracker,
        Consumer<Player> rocketJumpTracker,
        BukkitRuntimeRegistrations registrations
    ) {
        this.plugin = plugin;
        this.crossbowData = crossbowData;
        this.prismarineArrowItem = prismarineArrowItem;
        this.echoArrowItem = echoArrowItem;
        this.amethystArrowItem = amethystArrowItem;
        this.settings = settings;
        this.durabilityService = durabilityService;
        this.echoShardCooldownService = echoShardCooldownService;
        this.prismarineArrowTracker = prismarineArrowTracker;
        this.rocketJumpTracker = rocketJumpTracker;
        this.registrations = registrations;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (bow == null || bow.getType() != Material.CROSSBOW) {
            markPrismarineArrow(event);
            return;
        }

        var storedCharge = crossbowData.readCharge(bow);
        if (storedCharge.isEmpty()) {
            if (echoArrowItem.matches(event.getConsumable())) {
                if (!tryUseEchoShard(player, event)) {
                    return;
                }
                event.setCancelled(true);
                event.getProjectile().remove();
                fireSonicBeam(player);
                durabilityService.applyDirect(bow, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.ECHO_SHARD));
                crossbowData.clearNativeAppearance(bow);
                return;
            }
            if (amethystArrowItem.matches(event.getConsumable())) {
                applyAmethystArrow(event);
                durabilityService.record(player, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.AMETHYST_SHARD));
                crossbowData.clearNativeAppearance(bow);
                return;
            }
            if (event.getConsumable() != null && event.getConsumable().getType() == Material.FIREWORK_ROCKET) {
                if (!player.isOnGround()) {
                    event.setCancelled(true);
                    event.getProjectile().remove();
                    markPrismarineArrow(event);
                    return;
                }
                applyRocketJump(player, nativeFireworkCharge(event.getConsumable(), bow));
                durabilityService.record(player, CustomCrossbowRules.durabilityCost(CustomCrossbowChargeType.FIREWORK_ROCKET));
            }
            markPrismarineArrow(event);
            return;
        }

        StoredCrossbowCharge charge = storedCharge.get();
        CustomCrossbowShotPlan plan = CustomCrossbowRules.planFor(charge.type());
        if (charge.type() == CustomCrossbowChargeType.ECHO_SHARD && !tryUseEchoShard(player, event)) {
            return;
        }
        if (plan.cancelVanillaProjectile()) {
            event.setCancelled(true);
            event.getProjectile().remove();
        }

        switch (charge.type()) {
            case ECHO_SHARD -> fireSonicBeam(player);
            case FIREWORK_ROCKET -> {
                if (!player.isOnGround()) {
                    event.setCancelled(true);
                    event.getProjectile().remove();
                    return;
                }
                applyRocketJump(player, charge);
            }
            case AMETHYST_SHARD -> applyAmethystArrow(event);
            case ENDER_PEARL -> fireEnderPearl(player, plan);
        }
        applyDurability(player, bow, charge.type(), plan.cancelVanillaProjectile());
        crossbowData.clearCharge(bow);
        writeBackCrossbow(player, event.getHand(), bow);
    }

    private StoredCrossbowCharge nativeFireworkCharge(ItemStack firework, ItemStack crossbow) {
        int power = 1;
        if (firework.getItemMeta() instanceof org.bukkit.inventory.meta.FireworkMeta meta && meta.hasPower()) {
            power = meta.getPower();
        }
        int chargeCount = crossbow.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.MULTISHOT) > 0 ? 3 : 1;
        return new StoredCrossbowCharge(CustomCrossbowChargeType.FIREWORK_ROCKET, power, chargeCount);
    }

    private void markPrismarineArrow(EntityShootBowEvent event) {
        if (!prismarineArrowItem.matches(event.getConsumable())) {
            return;
        }
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }

        arrow.getPersistentDataContainer().set(prismarineArrowItem.getItemKey(), PersistentDataType.BYTE, (byte) 1);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        boolean underwater = isUnderwater(arrow);
        arrow.setDamage(CustomCrossbowRules.NORMAL_ARROW_DAMAGE);
        arrow.setVelocity(arrow.getVelocity().multiply(PrismarineArrowBalance.velocityMultiplier(underwater)));
        prismarineArrowTracker.accept(arrow);
        if (event.getEntity() instanceof Player player) {
            durabilityService.record(player, CustomCrossbowRules.prismarineArrowDurabilityCost());
        }
    }

    private void applyAmethystArrow(EntityShootBowEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        arrow.setDamage(CustomCrossbowRules.amethystDamage(CustomCrossbowRules.NORMAL_ARROW_DAMAGE));
        arrow.setCritical(false);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    }

    private void applyRocketJump(Player player, StoredCrossbowCharge charge) {
        float pitch = player.getLocation().getPitch();
        if (pitch < 30.0f) {
            return;
        }
        Vector velocity = player.getVelocity();
        velocity.setY(Math.max(velocity.getY(), CustomCrossbowRules.rocketJumpVelocity(charge.fireworkPower(), charge.chargeCount())));
        player.setVelocity(velocity);
        player.setFallDistance(0.0f);
        rocketJumpTracker.accept(player);
    }

    private void fireEnderPearl(Player player, CustomCrossbowShotPlan plan) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        EnderPearl pearl = player.getWorld().spawn(eye.add(direction.clone().multiply(0.6)), EnderPearl.class);
        pearl.setShooter(player);
        pearl.setVelocity(direction.multiply(plan.velocityMultiplier()));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 0.85f);
    }

    private void fireSonicBeam(Player player) {
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        if (world == null) {
            return;
        }

        Vector direction = eye.getDirection().normalize();
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.15f);
        world.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.0f);
        double beamRange = blockedBeamRange(world, eye, direction);
        emitSonicTrail(world, eye, direction, beamRange);

        for (Entity nearby : world.getNearbyEntities(beamBounds(eye, direction, beamRange), entity -> entity instanceof LivingEntity && !entity.equals(player))) {
            if (!(nearby instanceof LivingEntity target) || nearby.equals(player)) {
                continue;
            }
            if (!isInsideBeam(eye, direction, target.getLocation().add(0.0, target.getHeight() * 0.5, 0.0), beamRange)) {
                continue;
            }
            damageIgnoringArmor(player, target, settings.sonicDamage());
        }
    }

    private double blockedBeamRange(World world, Location start, Vector direction) {
        RayTraceResult blockHit = world.rayTraceBlocks(start, direction, settings.sonicRange(), FluidCollisionMode.NEVER, true);
        if (blockHit == null || blockHit.getHitPosition() == null) {
            return settings.sonicRange();
        }
        return Math.max(0.5, blockHit.getHitPosition().distance(start.toVector()));
    }

    private BoundingBox beamBounds(Location start, Vector direction, double beamRange) {
        Location end = start.clone().add(direction.clone().multiply(beamRange));
        return BoundingBox.of(start, end).expand(settings.sonicRadius() + 1.0);
    }

    private void emitSonicParticles(World world, Location start, Vector direction, double beamRange) {
        for (double distance = 0.5; distance <= beamRange; distance += 1.0) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void emitSonicTrail(World world, Location start, Vector direction, double beamRange) {
        SonicTrailPlan plan = SonicTrailPlan.create(beamRange);
        for (int tick = 0; tick < plan.ticks(); tick++) {
            int currentTick = tick;
            registrations.scheduleDelayed(
                "sonic-trail",
                () -> emitSonicTrailTick(world, start, direction, plan, currentTick),
                currentTick
            );
        }
    }

    private void emitSonicTrailTick(World world, Location start, Vector direction, SonicTrailPlan plan, int tick) {
        int startIndex = tick * plan.pointsPerTick();
        for (int i = 0; i < plan.pointsPerTick(); i++) {
            double distance = (startIndex + i + 1) * plan.spacing();
            Location point = start.clone().add(direction.clone().multiply(distance));
            world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticle(Particle.SCULK_SOUL, point, 2, 0.08, 0.08, 0.08, 0.0);
            world.spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.06, 0.06, 0.06, 0.0);
        }
    }

    private boolean isInsideBeam(Location start, Vector direction, Location target, double beamRange) {
        Vector offset = target.toVector().subtract(start.toVector());
        double alongBeam = offset.dot(direction);
        if (alongBeam < 0.0 || alongBeam > beamRange) {
            return false;
        }

        Vector closest = start.toVector().add(direction.clone().multiply(alongBeam));
        return closest.distance(target.toVector()) <= settings.sonicRadius();
    }

    private void damageIgnoringArmor(Player shooter, LivingEntity target, double damage) {
        int previousNoDamageTicks = target.getNoDamageTicks();
        target.setNoDamageTicks(0);
        target.damage(0.01, shooter);
        target.setNoDamageTicks(0);
        target.setHealth(Math.max(0.0, target.getHealth() - damage));
        target.setNoDamageTicks(previousNoDamageTicks);
    }

    private boolean tryUseEchoShard(Player player, EntityShootBowEvent event) {
        long currentTick = player.getWorld().getGameTime();
        if (echoShardCooldownService.tryUse(player.getUniqueId(), currentTick)) {
            return true;
        }

        event.setCancelled(true);
        event.getProjectile().remove();
        long remaining = echoShardCooldownService.remainingTicks(player.getUniqueId(), currentTick);
        player.sendMessage("§cEcho Shard en cooldown: " + Math.max(1, (long) Math.ceil(remaining / 20.0)) + "s.");
        return false;
    }

    private void applyDurability(Player player, ItemStack crossbow, CustomCrossbowChargeType chargeType, boolean direct) {
        int cost = CustomCrossbowRules.durabilityCost(chargeType);
        if (direct) {
            durabilityService.applyDirect(crossbow, cost);
            return;
        }
        durabilityService.record(player, cost);
    }

    private boolean isUnderwater(AbstractArrow arrow) {
        return arrow.getLocation().getBlock().getType() == Material.WATER;
    }

    private void writeBackCrossbow(Player player, EquipmentSlot hand, ItemStack crossbow) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(crossbow);
            return;
        }
        player.getInventory().setItemInMainHand(crossbow);
    }
}
