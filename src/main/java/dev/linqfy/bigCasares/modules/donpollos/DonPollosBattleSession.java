package dev.linqfy.bigCasares.modules.donpollos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estado de una batalla de baile que no depende de Bukkit ni de Adventure: quien pelea, contra que Salsero,
 * el premio que se le debe y las tareas programadas de esa sesion (para poder cancelarlas).
 * El manager la extiende con lo que si necesita el servidor (villager, barra, input).
 */
class DonPollosBattleSession {

    final UUID playerId;
    final UUID salseroId;
    final DonPollosDanceBattle battle;
    boolean over;
    /** Items del premio que ya se ganaron pero todavia no se entregaron (si el jugador sale, se guardan). */
    int owedReward;
    private final List<Runnable> cancelers = new ArrayList<>();
    private boolean cancelled;

    DonPollosBattleSession(UUID playerId, UUID salseroId, DonPollosDanceBattle battle) {
        this.playerId = playerId;
        this.salseroId = salseroId;
        this.battle = battle;
    }

    boolean active() {
        return !over;
    }

    /** Registra como cancelar una tarea de esta sesion (caminata, tiros del premio). */
    synchronized void track(Runnable canceler) {
        if (cancelled) {
            canceler.run();
            return;
        }
        cancelers.add(canceler);
    }

    synchronized boolean tasksCancelled() {
        return cancelled;
    }

    /** Cancela todas las tareas de la sesion; las que se registren despues se cancelan al instante. */
    void cancelTasks() {
        List<Runnable> pending;
        synchronized (this) {
            cancelled = true;
            pending = new ArrayList<>(cancelers);
            cancelers.clear();
        }
        for (Runnable canceler : pending) {
            canceler.run();
        }
    }
}
