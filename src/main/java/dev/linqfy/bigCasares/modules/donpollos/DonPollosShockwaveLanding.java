package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Predicate;

/**
 * Los bloques que lanza el megasalto no pueden aterrizar en una zona protegida ni en un cubo activo (adentro tampoco):
 * si caen ahi se cancela y el bloque desaparece sin soltar item.
 */
final class DonPollosShockwaveLanding implements Listener {

    private final NamespacedKey flyingKey;
    private final DonPollosProtection protection;
    private final Predicate<Block> cubeBlock;

    DonPollosShockwaveLanding(NamespacedKey flyingKey, DonPollosProtection protection, Predicate<Block> cubeBlock) {
        this.flyingKey = flyingKey;
        this.protection = protection;
        this.cubeBlock = cubeBlock;
    }

    /** Si un bloque lanzado por una onda no puede aterrizar en {@code landing}. */
    static boolean forbidden(Block landing, DonPollosProtection protection, Predicate<Block> cubeBlock) {
        return cubeBlock.test(landing) || protection.isProtected(landing);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onLand(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof FallingBlock) || !entity.getPersistentDataContainer().has(flyingKey, PersistentDataType.BYTE)) {
            return;
        }
        if (forbidden(event.getBlock(), protection, cubeBlock)) {
            event.setCancelled(true);
            entity.remove();
        }
    }
}
