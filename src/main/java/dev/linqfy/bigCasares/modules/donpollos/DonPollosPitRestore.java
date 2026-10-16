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
    /**
     * Si dos cubos tocan los mismos bloques. Cada cubo ocupa su chunk mas un anillo de paredes de 1 bloque alrededor,
     * desde un bloque abajo del fondo hasta el suelo original: cubos en chunks vecinos (tambien en diagonal) comparten
     * paredes. Entre cubos de una misma pelea eso se resuelve restaurando en orden inverso; entre bosses distintos,
     * que restauran en momentos distintos, no se puede: por eso no se cava un cubo que toque el de otro boss.
     */
    static boolean regionsOverlap(int chunkXA, int chunkZA, int bottomA, int feetA,
                                  int chunkXB, int chunkZB, int bottomB, int feetB) {
        boolean neighbours = Math.abs(chunkXA - chunkXB) <= 1 && Math.abs(chunkZA - chunkZB) <= 1;
        boolean sameHeights = bottomA - 1 <= feetB && bottomB - 1 <= feetA;
        return neighbours && sameHeights;
    }

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
