package dev.linqfy.bigCasares.modules.donpollos;

import java.util.ArrayList;
import java.util.List;

/** Orden para dejar el terreno como estaba despues de las Batallas del Cubo (sin Bukkit). */
final class DonPollosPitRestore {

    private DonPollosPitRestore() {
    }

    /**
     * Los cubos de chunks vecinos comparten paredes: el segundo guarda como "original" un bloque que el primero ya
     * habia cambiado. Deshaciendo todo en orden inverso (ultimo cubo primero, ultimo cambio primero) cada bloque
     * termina con el estado que tenia antes del primer cambio.
     */
    static <T> List<T> restoreOrder(List<? extends List<T>> changesPerPit) {
        List<T> order = new ArrayList<>();
        for (int pit = changesPerPit.size() - 1; pit >= 0; pit--) {
            List<T> changes = changesPerPit.get(pit);
            for (int change = changes.size() - 1; change >= 0; change--) {
                order.add(changes.get(change));
            }
        }
        return order;
    }
}
