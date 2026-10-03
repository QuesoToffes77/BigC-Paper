package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;

/**
 * Listens for Acid Rain mob deaths. Death triggers the runtime's model release
 * and {@code nitric_acid} drop logic. Identity comes from persistent-data tags,
 * never from a display name.
 */
final class AcidRainMobListener implements Listener {
    private final AcidRainRuntime runtime;

    AcidRainMobListener(AcidRainRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        AcidRainMobTagStore store =
            new BukkitAcidRainMobTagStore(event.getEntity().getPersistentDataContainer());
        if (!AcidRainMobIdentity.isAcidRainMob(store)) {
            return;
        }
        runtime.onMobDeath(event.getEntity(), store, event.getDrops());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        LivingEntity attacker = attacker(event);
        if (attacker != null && isAcidMob(attacker)) {
            runtime.onMobAttack(attacker);
        }
        if (event.getEntity() instanceof LivingEntity victim && isAcidMob(victim)) {
            runtime.onMobHurt(victim);
        }
    }

    private static LivingEntity attacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity living) {
            return living;
        }
        if (event.getDamager() instanceof Projectile projectile
            && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    private static boolean isAcidMob(LivingEntity entity) {
        return AcidRainMobIdentity.isAcidRainMob(
            new BukkitAcidRainMobTagStore(entity.getPersistentDataContainer()));
    }
}
