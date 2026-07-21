package dev.linqfy.bigCasares.modules.danger;

import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.function.Consumer;

public final class DangerGameplayListener implements Listener {
    private static final Set<EntityRegainHealthEvent.RegainReason> TIER_THREE_HEALING = Set.of(
        EntityRegainHealthEvent.RegainReason.REGEN,
        EntityRegainHealthEvent.RegainReason.SATIATED,
        EntityRegainHealthEvent.RegainReason.EATING,
        EntityRegainHealthEvent.RegainReason.MAGIC,
        EntityRegainHealthEvent.RegainReason.MAGIC_REGEN
    );

    private final DangerService service;
    private final Consumer<java.util.UUID> presentationRefresh;

    public DangerGameplayListener(DangerService service, Consumer<java.util.UUID> presentationRefresh) {
        this.service = service;
        this.presentationRefresh = presentationRefresh;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        DangerSnapshot snapshot = service.beginSession(event.getPlayer().getUniqueId());
        if (snapshot.inactiveDaysApplied() > 0) {
            event.getPlayer().sendMessage("§7Tu peligro se redujo por " + snapshot.inactiveDaysApplied()
                + " día(s) de inactividad. Ahora tenés §f" + snapshot.totalScore() + "§7 puntos.");
        }
        presentationRefresh.accept(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        service.touch(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        var victim = event.getEntity();
        var killer = victim.getKiller();
        DangerTier tierBeforeDeath = service.snapshot(victim.getUniqueId()).tier();
        if (tierBeforeDeath.level() >= 2) {
            event.setDroppedExp((int) Math.floor(event.getDroppedExp() * 0.90));
        }
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            DangerKillResult kill = service.recordKill(killer.getUniqueId(), victim.getUniqueId());
            killer.sendMessage("§eGanaste §f" + kill.awardedActivity() + " §epuntos de peligro.");
            if (tierBeforeDeath == DangerTier.LETAL) {
                killer.giveExp(25);
                killer.sendMessage("§cObjetivo letal eliminado: §f+25 XP§c.");
            }
            
            // VENTAJA PELIGRO: KILLER EFFECTS
            DangerTier killerTier = service.snapshot(killer.getUniqueId()).tier();
            if (killerTier == DangerTier.PELIGROSO) {
                killer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 3 * 20, 1));
            } else if (killerTier == DangerTier.LETAL) {
                double maxHealth = killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null 
                    ? killer.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() 
                    : 20.0;
                killer.setHealth(Math.min(maxHealth, killer.getHealth() + 4.0));
                killer.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 3 * 20, 0));
            }

            presentationRefresh.accept(killer.getUniqueId());
        }
        service.recordDeath(victim.getUniqueId(), killer == null ? null : killer.getUniqueId());
        presentationRefresh.accept(victim.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Player player)) return;
        DangerTier tier = service.snapshot(player.getUniqueId()).tier();
        boolean reduce = tier == DangerTier.PELIGROSO
            ? event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
            : tier == DangerTier.LETAL && TIER_THREE_HEALING.contains(event.getRegainReason());
        if (reduce) event.setAmount(event.getAmount() * 0.85);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExhaustion(EntityExhaustionEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player
            && service.snapshot(player.getUniqueId()).tier().level() >= 2) {
            event.setExhaustion(event.getExhaustion() * 1.15f);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShieldDisable(PlayerShieldDisableEvent event) {
        if (service.snapshot(event.getPlayer().getUniqueId()).tier() == DangerTier.LETAL) {
            event.setCooldown((int) Math.ceil(event.getCooldown() * 1.15));
        }
    }
}
