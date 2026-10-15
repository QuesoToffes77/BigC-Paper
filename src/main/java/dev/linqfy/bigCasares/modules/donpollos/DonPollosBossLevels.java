package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Collection;

/**
 * Nivel de dificultad del boss al invocarlo (sin Bukkit): el mas alto entre las victorias de quien lo invoco (leidas
 * al poner el bloque, aunque despues se desconecte) y las de cada jugador conectado cerca del lugar de invocacion.
 * Asi una cuenta nueva no puede invocar un boss facil para que se lleve el premio un veterano.
 */
final class DonPollosBossLevels {

    private DonPollosBossLevels() {
    }

    static int levelFor(int summonerWins, Collection<Integer> nearbyWins) {
        int level = Math.max(0, summonerWins);
        for (int wins : nearbyWins) {
            level = Math.max(level, wins);
        }
        return level;
    }
}
