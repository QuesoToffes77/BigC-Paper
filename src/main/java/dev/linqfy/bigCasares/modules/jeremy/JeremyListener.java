package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

final class JeremyListener implements Listener {
    private final JeremyRuntime runtime;

    JeremyListener(JeremyRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (runtime.isCurrentJeremy(event.getDamager())) {
            if (runtime.shouldCancelJeremyAttack(event.getDamager(), event.getEntity())) {
                event.setCancelled(true);
                return;
            }
            if (event.getEntity() instanceof Player target) {
                event.setDamage(runtime.meleeDamage(event.getDamager(), target, event.getDamage()));
            }
        }
        if (event.getDamager() instanceof Player player && runtime.isCurrentJeremy(event.getEntity())) {
            event.setDamage(runtime.incomingDamage(player, event.getEntity(), event.getDamage()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageApplied(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player target && event.getFinalDamage() > 0.0) {
            runtime.recordJeremyDamage(event.getDamager(), target);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        runtime.enforceTarget(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        runtime.onEntityDeath(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        runtime.onTargetQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        runtime.onTargetWorldChange(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        runtime.onWorldUnload(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        runtime.onEntitiesLoaded(event.getEntities());
    }
}
