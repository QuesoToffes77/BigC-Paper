package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Que pasa cuando se cierra el menu del secreto del Gordito (sin Paper, asi se prueba con un InventoryCloseEvent
 * real): avisa que se cerro y, si no se termino, devuelve todo lo que el jugador habia puesto.
 */
final class DonPollosSecretMenuCloser {

    /** El holder del menu del secreto: lo puesto y si ya se uso para el ritual. */
    interface Session extends InventoryHolder {
        boolean done();

        Set<DonPollosSecretOffering> placed();
    }

    private final DonPollosSecretAltar altar;

    DonPollosSecretMenuCloser(DonPollosSecretAltar altar) {
        this.altar = altar;
    }

    /** {@code closed} se avisa siempre; {@code drop} recibe lo que no entra en el inventario. */
    void handle(InventoryCloseEvent event, Consumer<Player> closed, java.util.function.BiConsumer<Player, ItemStack> drop) {
        if (!(event.getInventory().getHolder() instanceof Session session) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        closed.accept(player);
        if (!session.done()) {
            altar.returnAll(player.getInventory(), session.placed(), leftover -> drop.accept(player, leftover));
        }
    }
}
