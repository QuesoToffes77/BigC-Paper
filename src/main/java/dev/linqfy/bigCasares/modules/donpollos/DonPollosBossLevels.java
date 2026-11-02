package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;

/**
 * Nivel de dificultad del boss (solo Bukkit, sin Adventure): el mas alto entre las victorias de quien lo invoco (leidas
 * al poner el bloque, aunque despues se desconecte) y las de cada jugador conectado cerca del lugar de invocacion.
 * Despues, si un jugador mas fuerte le pega, el boss sube a su nivel. Asi una cuenta nueva no puede invocar un boss
 * facil para que se lo lleve un veterano.
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

    /** Victorias guardadas en el jugador (se leen al poner el bloque del secreto). */
    static int winsOf(PersistentDataContainer player, NamespacedKey winsKey) {
        return player.getOrDefault(winsKey, PersistentDataType.INTEGER, 0);
    }

    /** Nivel al invocar: el invocador (ya capturado, este o no conectado) y los conectados en supervivencia/aventura cerca. */
    static int levelAt(Location spawn, int summonerWins, Collection<? extends Player> online, NamespacedKey winsKey,
                       double radius) {
        int level = Math.max(0, summonerWins);
        for (Player player : online) {
            if (player.isDead() || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR
                || player.getLocation().distanceSquared(spawn) > radius * radius) {
                continue;
            }
            level = Math.max(level, winsOf(player.getPersistentDataContainer(), winsKey));
        }
        return level;
    }

    /** Si quien le pega tiene mas victorias que el nivel del boss, el boss sube a ese nivel (nunca baja). */
    static int levelAfterHit(int current, int attackerWins) {
        return Math.max(current, attackerWins);
    }
}
