package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosCubeEscapeTest {

    // cubo en el chunk (2, -1): x 32..47, z -16..-1, piso original en y 70, fondo en y 64
    private static boolean inCube(int x, double y, int z) {
        return DonPollosCubeEscape.inside(2, -1, 70, 64, x, z, y);
    }

    @Test
    void theCubeIsTheChunkColumnsUpToTheOriginalGround() {
        assertTrue(inCube(32, 64, -16));
        assertTrue(inCube(47, 70.9, -1));
        assertTrue(inCube(40, 63, -8));
        assertFalse(inCube(40, 71, -8), "arriba del suelo original ya es afuera");
        assertFalse(inCube(48, 66, -8), "el chunk de al lado");
        assertFalse(inCube(40, 66, 0), "el chunk de al lado");
    }

    @Test
    void flyingOutTheTopOrTeleportingAwayIsAnEscape() {
        // elytras / tridente / carga de viento: de adentro a arriba del cubo
        assertTrue(DonPollosCubeEscape.escapes(inCube(40, 69, -8), inCube(40, 71.2, -8)));
        // perla, chorus o /home: de adentro a cualquier lado afuera
        assertTrue(DonPollosCubeEscape.escapes(inCube(40, 66, -8), inCube(500, 80, 500)));
        // moverse o tirar una perla dentro del mismo cubo esta bien
        assertFalse(DonPollosCubeEscape.escapes(inCube(40, 66, -8), inCube(33, 64, -15)));
        // entrar al cubo (o caerse adentro) tambien
        assertFalse(DonPollosCubeEscape.escapes(inCube(40, 75, -8), inCube(40, 69, -8)));
    }

    @Test
    void pearlsAndWindChargesCannotBeThrownInside() {
        assertTrue(DonPollosCubeEscape.blocksProjectile("ENDER_PEARL"));
        assertTrue(DonPollosCubeEscape.blocksProjectile("WIND_CHARGE"));
        assertFalse(DonPollosCubeEscape.blocksProjectile("ARROW"));
        assertFalse(DonPollosCubeEscape.blocksProjectile("SNOWBALL"));
    }

    @Test
    void anEmptyCubeIsClosedAfterThirtySeconds() {
        assertFalse(DonPollosCubeEscape.abandoned(-1, 10_000), "hay alguien adentro");
        assertFalse(DonPollosCubeEscape.abandoned(1000, 1000 + 599));
        assertTrue(DonPollosCubeEscape.abandoned(1000, 1000 + 600));
    }
}
