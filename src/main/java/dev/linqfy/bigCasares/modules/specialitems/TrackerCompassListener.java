package dev.linqfy.bigCasares.modules.specialitems;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class TrackerCompassListener implements Listener {

    private final TrackerCompassItem item;
    private final TrackerCompassService service;
    private final LongSupplier clock;

    public TrackerCompassListener(TrackerCompassItem item, TrackerCompassService service) {
        this(item, service, System::currentTimeMillis);
    }

    TrackerCompassListener(
        TrackerCompassItem item,
        TrackerCompassService service,
        LongSupplier clock
    ) {
        this.item = Objects.requireNonNull(item, "item");
        this.service = Objects.requireNonNull(service, "service");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)
            || !item.matches(attacker.getInventory().getItemInMainHand())) {
            return;
        }
        long now = clock.getAsLong();
        if (service.startTracking(attacker.getUniqueId(), target.getUniqueId(), now)) {
            attacker.sendMessage("§bAhora estás rastreando a §f" + target.getName() + "§b por 45 segundos.");
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.5f);
            return;
        }
        TrackerCompassState state = service.state(attacker.getUniqueId(), now);
        String reason = state != null && state.phase() == TrackerCompassState.Phase.TRACKING
            ? "§eYa estás rastreando a otro jugador."
            : "§cLa brújula todavía está recargando.";
        attacker.sendMessage(reason);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.invalidateTarget(event.getPlayer().getUniqueId(), clock.getAsLong());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        service.invalidateTarget(event.getEntity().getUniqueId(), clock.getAsLong());
    }
}
