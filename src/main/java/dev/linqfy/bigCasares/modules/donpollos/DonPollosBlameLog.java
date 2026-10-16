package dev.linqfy.bigCasares.modules.donpollos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A quien se le echa la culpa de la muerte de cada jugador (sin Bukkit ni Adventure, asi se puede probar). Los ataques
 * especiales (laser, meteorito, rayo, onda) hacen el dano con un evento de "el boss te pego": mientras dura ese dano
 * hay un contexto de ataque especial y la embestida (RAM) no pisa la causa ni dispara la animacion de golpe.
 */
final class DonPollosBlameLog<C> {

    record Hit<C>(C cause, long tick) {
    }

    private final Map<UUID, Hit<C>> hits = new HashMap<>();
    /** Ataques especiales con el dano en curso (un contador, por si uno hace dano dentro de otro). */
    private int specialDepth;

    void blame(UUID player, C cause, long tick) {
        hits.put(player, new Hit<>(cause, tick));
    }

    /** Anota la causa y corre el dano del ataque especial dentro de su contexto (se limpia siempre). */
    void special(UUID player, C cause, long tick, Runnable damage) {
        blame(player, cause, tick);
        specialDepth++;
        try {
            damage.run();
        } finally {
            specialDepth--;
        }
    }

    boolean specialInProgress() {
        return specialDepth > 0;
    }

    /** Embestida real del boss: solo cuenta si no es el dano de un ataque especial. Devuelve si la anoto. */
    boolean ram(UUID player, C ramCause, long tick) {
        if (specialInProgress()) {
            return false;
        }
        blame(player, ramCause, tick);
        return true;
    }

    /** Saca (y olvida) la culpa de un jugador; null si no hay o ya paso demasiado tiempo. */
    C take(UUID player, long now, long maxAge) {
        Hit<C> hit = hits.remove(player);
        return hit == null || now - hit.tick() > maxAge ? null : hit.cause();
    }
}
