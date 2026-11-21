package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Uses normal damage events so protection plugins, attribution and totems still work. */
@SuppressWarnings("removal")
final class SniperDamageService implements Listener {

    private final Map<UUID, Hit> pending = new HashMap<>();

    boolean damage(Player shooter, LivingEntity target, double damage) {
        Hit hit = new Hit(shooter, damage);
        Hit previous = pending.put(target.getUniqueId(), hit);
        int immunity = target.getNoDamageTicks();
        try {
            target.setNoDamageTicks(0);
            target.damage(damage, shooter);
            return hit.accepted;
        } finally {
            target.setNoDamageTicks(immunity);
            if (previous == null) {
                pending.remove(target.getUniqueId());
            } else {
                pending.put(target.getUniqueId(), previous);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Hit hit = pending.get(event.getEntity().getUniqueId());
        if (event.isCancelled() || hit == null || hit.event != null || !hit.shooter.equals(event.getDamager())
            || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        hit.event = event;
        boolean blocked = event.isApplicable(DamageModifier.BLOCKING)
            && event.getDamage(DamageModifier.BLOCKING) < 0;
        double shieldReduction = blocked ? hit.damage * 0.5 : 0.0;
        double absorption = Math.min(hit.damage - shieldReduction, target.getAbsorptionAmount());
        event.setDamage(hit.damage);
        for (DamageModifier modifier : DamageModifier.values()) {
            if (modifier == DamageModifier.BASE || !event.isApplicable(modifier)) {
                continue;
            }
            double reduction = switch (modifier) {
                case BLOCKING -> shieldReduction;
                case ABSORPTION -> absorption;
                default -> 0.0;
            };
            event.setDamage(modifier, -reduction);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamageResult(EntityDamageByEntityEvent event) {
        Hit hit = pending.get(event.getEntity().getUniqueId());
        if (hit != null && hit.event == event) {
            hit.accepted = !event.isCancelled() && event.getFinalDamage() > 0;
        }
    }

    private static final class Hit {
        private final Player shooter;
        private final double damage;
        private EntityDamageByEntityEvent event;
        private boolean accepted;

        private Hit(Player shooter, double damage) {
            this.shooter = shooter;
            this.damage = damage;
        }
    }
}
