package dev.linqfy.bigCasares.modules.alexscaves.toxic;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** Tells whether a hit really reached its victim, so radiation is only applied where protections let damage through. */
final class DamageProbe {
    private DamageProbe() {
    }

    /** Health plus absorption: what a hit takes from. */
    static double pool(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    /** A hit landed when it took something from the victim or killed it. */
    static boolean landed(double before, double after, boolean dead) {
        return dead || after < before;
    }

    /**
     * For radiation that is not carried by a damage call (clouds, licks): whether {@code source} could hurt the victim.
     * Only players are protected by claims and zones, so every other victim is let through without raising an event.
     */
    static boolean mayHarm(Entity source, LivingEntity victim) {
        if (!(victim instanceof Player)) {
            return true;
        }
        if (source == null || !source.isValid()) {
            return false;
        }
        DamageSource damage = DamageSource.builder(DamageType.GENERIC).withCausingEntity(source).withDirectEntity(source).build();
        return new EntityDamageByEntityEvent(source, victim, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage, 1.0).callEvent();
    }
}
