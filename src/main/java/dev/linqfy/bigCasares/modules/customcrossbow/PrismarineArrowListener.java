package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PrismarineArrowListener implements Listener {

    private final PrismarineArrowItem prismarineArrowItem;
    private final Set<UUID> activeArrows = new LinkedHashSet<>();
    private final BukkitTask heartbeatTask;
    private boolean shutdown;

    public PrismarineArrowListener(
        PrismarineArrowItem prismarineArrowItem,
        BukkitRuntimeRegistrations registrations
    ) {
        this.prismarineArrowItem = prismarineArrowItem;
        this.heartbeatTask = registrations.scheduleRepeating("prismarine-heartbeat", this::heartbeat, 1L, 1L);
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        if (!heartbeatTask.isCancelled()) {
            heartbeatTask.cancel();
        }
        activeArrows.clear();
    }

    public void track(AbstractArrow arrow) {
        activeArrows.add(arrow.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow) || !isPrismarineArrow(arrow)) {
            return;
        }

        event.setDamage(PrismarineArrowBalance.damage(CustomCrossbowRules.NORMAL_ARROW_DAMAGE, isUnderwater(arrow)));
    }

    private void heartbeat() {
        Iterator<UUID> iterator = activeArrows.iterator();
        while (iterator.hasNext()) {
            Entity entity = Bukkit.getEntity(iterator.next());
            if (!(entity instanceof AbstractArrow arrow) || arrow.isDead() || arrow.isInBlock()) {
                iterator.remove();
                continue;
            }
            if (!isPrismarineArrow(arrow) || !isUnderwater(arrow)) {
                continue;
            }

            Vector velocity = arrow.getVelocity();
            if (velocity.lengthSquared() <= 0.01) {
                continue;
            }
            double targetSpeed = PrismarineArrowBalance.velocityMultiplier(true);
            if (velocity.length() < targetSpeed) {
                arrow.setVelocity(velocity.normalize().multiply(targetSpeed));
            }
            arrow.setDamage(PrismarineArrowBalance.damage(CustomCrossbowRules.NORMAL_ARROW_DAMAGE, true));
        }
    }

    private boolean isPrismarineArrow(AbstractArrow arrow) {
        Byte marker = arrow.getPersistentDataContainer().get(prismarineArrowItem.getItemKey(), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private boolean isUnderwater(AbstractArrow arrow) {
        return arrow.getLocation().getBlock().getType() == Material.WATER;
    }
}
