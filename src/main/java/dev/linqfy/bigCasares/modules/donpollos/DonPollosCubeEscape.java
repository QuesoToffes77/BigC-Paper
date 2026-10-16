package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Set;

/** Reglas para que nadie se escape del cubo durante las Batallas del Cubo (sin Bukkit). */
final class DonPollosCubeEscape {

    /** Si el cubo queda sin nadie adentro este tiempo (30 s), se terminan las Batallas del Cubo. */
    static final long ABANDON_TICKS = 30 * 20;

    /** Proyectiles que sirven para salir: la perla te teletransporta y la carga de viento te lanza. */
    private static final Set<String> ESCAPE_PROJECTILES = Set.of("ENDER_PEARL", "WIND_CHARGE");

    private DonPollosCubeEscape() {
    }

    /**
     * Si (x, y, z) esta adentro del cubo vaciado en el chunk (chunkX, chunkZ): las columnas del chunk, desde un
     * bloque abajo del piso hasta la altura original del suelo. Por los costados estan las paredes, asi que la unica
     * salida es por arriba.
     */
    static boolean inside(int chunkX, int chunkZ, int feet, int bottom, int blockX, int blockZ, double y) {
        return blockX >> 4 == chunkX && blockZ >> 4 == chunkZ && y < feet + 1 && y >= bottom - 1;
    }

    /** Un movimiento o teletransporte que sale del cubo (empieza adentro y termina afuera). */
    static boolean escapes(boolean fromInside, boolean toInside) {
        return fromInside && !toInside;
    }

    static boolean blocksProjectile(String entityType) {
        return ESCAPE_PROJECTILES.contains(entityType);
    }

    /** emptySince: tick desde el que no hay nadie en el cubo, o -1 si hay alguien. */
    static boolean abandoned(long emptySince, long now) {
        return emptySince >= 0 && now - emptySince >= ABANDON_TICKS;
    }
}
