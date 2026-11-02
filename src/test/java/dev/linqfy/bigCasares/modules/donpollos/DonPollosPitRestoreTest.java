package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosPitRestoreTest {

    /** Un cambio guardado: la posicion y como estaba el bloque justo antes de tocarlo. */
    private record Change(int x, int y, String before) {
    }

    /**
     * Mundo de juguete de 2 dimensiones (x, y): dos cubos de chunks vecinos, cada uno vacia sus columnas y pone
     * paredes de ladrillo en el borde, incluida la columna que comparten (x = 16).
     */
    private static Map<String, String> terrain() {
        Map<String, String> world = new HashMap<>();
        for (int x = -1; x <= 32; x++) {
            for (int y = 0; y < 6; y++) {
                world.put(x + "," + y, y == 0 ? "stone" : (x % 3 == 0 ? "dirt" : "grass"));
            }
        }
        return world;
    }

    private static List<Change> dig(Map<String, String> world, int fromX, int toX) {
        List<Change> changes = new ArrayList<>();
        for (int y = 0; y < 6; y++) {
            for (int x = fromX; x <= toX; x++) {
                String key = x + "," + y;
                changes.add(new Change(x, y, world.get(key)));
                world.put(key, x == fromX || x == toX || y == 0 ? "stone_bricks" : "air");
            }
        }
        return changes;
    }

    private static void apply(Map<String, String> world, List<Change> order) {
        for (Change change : order) {
            world.put(change.x() + "," + change.y(), change.before());
        }
    }

    @Test
    void adjacentPitsSharingAWallRestoreTheOriginalTerrain() {
        Map<String, String> original = terrain();
        Map<String, String> world = new HashMap<>(original);
        List<List<Change>> pits = List.of(dig(world, -1, 16), dig(world, 16, 32));

        apply(world, DonPollosPitRestore.restoreOrder(pits));

        assertEquals(original, world);
    }

    @Test
    void restoringInDigOrderWouldLeaveTheSharedWallBroken() {
        // asi estaba antes del arreglo: el segundo cubo guarda la pared ya cambiada por el primero y la pisa
        Map<String, String> original = terrain();
        Map<String, String> world = new HashMap<>(original);
        List<List<Change>> pits = List.of(dig(world, -1, 16), dig(world, 16, 32));

        for (List<Change> pit : pits) {
            apply(world, pit);
        }

        assertNotEquals(original, world);
    }

    @Test
    void lastPitAndLastChangeAreUndoneFirst() {
        assertEquals(List.of("b2", "b1", "a2", "a1"),
            DonPollosPitRestore.restoreOrder(List.of(List.of("a1", "a2"), List.of("b1", "b2"))));
        assertEquals(List.of(), DonPollosPitRestore.restoreOrder(List.<List<String>>of()));
    }

    @Test
    void pitsInNeighbouringChunksShareWalls() {
        // mismo chunk, al lado, en diagonal: comparten bloques
        assertTrue(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 0, 0, 64, 70));
        assertTrue(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 1, 0, 64, 70));
        assertTrue(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, -1, -1, 64, 70));
        // con un chunk de por medio no se tocan
        assertFalse(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 2, 0, 64, 70));
        assertFalse(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 1, 3, 64, 70));
    }

    @Test
    void pitsAtDifferentHeightsDoNotTouch() {
        // uno de 64 a 70 y otro de 100 a 106, en chunks vecinos (un cubo en una montania)
        assertFalse(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 1, 0, 100, 106));
        // la pared de abajo del de arriba (fondo - 1) llega justo al suelo del de abajo
        assertTrue(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 1, 0, 71, 77));
        assertFalse(DonPollosPitRestore.regionsOverlap(0, 0, 64, 70, 1, 0, 72, 78));
    }
}
