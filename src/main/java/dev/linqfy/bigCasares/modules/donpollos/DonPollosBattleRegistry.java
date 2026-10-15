package dev.linqfy.bigCasares.modules.donpollos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Indice de batallas activas por jugador y por Salsero. Liberar una sesion solo toca sus propias entradas:
 * un callback viejo (tarea de victoria, jugador que se reconecto) no puede borrar el bloqueo de una batalla nueva.
 */
final class DonPollosBattleRegistry<S extends DonPollosBattleSession> {

    private final Map<UUID, S> byPlayer = new HashMap<>();
    private final Map<UUID, UUID> bySalsero = new HashMap<>();

    /** Registra la sesion si ni el jugador ni el Salsero estan en otra batalla. */
    boolean register(S session) {
        if (byPlayer.containsKey(session.playerId) || bySalsero.containsKey(session.salseroId)) {
            return false;
        }
        byPlayer.put(session.playerId, session);
        bySalsero.put(session.salseroId, session.playerId);
        return true;
    }

    S get(UUID playerId) {
        return byPlayer.get(playerId);
    }

    boolean isBattling(UUID playerId) {
        return byPlayer.containsKey(playerId);
    }

    boolean isBusy(UUID salseroId) {
        return bySalsero.containsKey(salseroId);
    }

    /** true si esta exacta sesion sigue siendo la vigente del jugador y de su Salsero. */
    boolean owns(S session) {
        return byPlayer.get(session.playerId) == session
            && session.playerId.equals(bySalsero.get(session.salseroId));
    }

    /** Libera la sesion solo si todavia es la vigente; devuelve false (sin tocar nada) si ya fue reemplazada o liberada. */
    boolean release(S session) {
        if (!owns(session)) {
            return false;
        }
        byPlayer.remove(session.playerId, session);
        bySalsero.remove(session.salseroId, session.playerId);
        return true;
    }

    List<S> sessions() {
        return new ArrayList<>(byPlayer.values());
    }
}
