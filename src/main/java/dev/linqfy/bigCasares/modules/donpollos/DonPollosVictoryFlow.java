package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Flujo de victoria contra el Salsero (sin Adventure ni servidor): camina hacia el jugador, tira el premio item por item
 * y libera la sesion. Cada callback comprueba que su sesion siga siendo la vigente, y el premio que falta se guarda en
 * el jugador si sale antes de recibirlo.
 */
final class DonPollosVictoryFlow<S extends DonPollosBattleSession> {

    /** Programador de tareas; devuelve como cancelarlas. */
    interface Scheduler {
        Runnable later(long delayTicks, Runnable task);

        Runnable timer(long periodTicks, Runnable task);
    }

    /** Lo que el flujo necesita del mundo para una sesion concreta. */
    interface World {
        boolean salseroValid();

        boolean playerOnline();

        boolean playerClose();

        void approach();

        void stopAndFace();

        void dropOne();

        void give(int amount);

        void endWithoutPlayer();

        void release();
    }

    static final long WALK_PERIOD = 5L;
    static final int WALK_MAX_TICKS = 100;
    static final long THROW_SPACING = 4L;

    private final DonPollosBattleRegistry<S> registry;
    private final Scheduler scheduler;

    DonPollosVictoryFlow(DonPollosBattleRegistry<S> registry, Scheduler scheduler) {
        this.registry = registry;
        this.scheduler = scheduler;
    }

    void begin(S session, World world) {
        int[] waited = {0};
        Runnable[] walk = new Runnable[1];
        walk[0] = scheduler.timer(WALK_PERIOD, () -> {
            if (!registry.owns(session) || session.tasksCancelled()) {
                // la sesion ya termino o fue reemplazada: esta tarea vieja no toca nada
                walk[0].run();
                return;
            }
            waited[0] += WALK_PERIOD;
            boolean online = world.playerOnline();
            boolean valid = world.salseroValid();
            if (online && valid && !world.playerClose() && waited[0] < WALK_MAX_TICKS) {
                world.approach();
                return;
            }
            walk[0].run();
            if (!online) {
                world.endWithoutPlayer();
            } else if (valid) {
                world.stopAndFace();
                if (session.owedReward > 0) {
                    throwReward(session, world);
                } else {
                    world.release();
                }
            } else {
                giveOwed(session, world);
                world.release();
            }
        });
        session.track(walk[0]);
    }

    private void throwReward(S session, World world) {
        int amount = session.owedReward;
        for (int i = 0; i < amount; i++) {
            session.track(scheduler.later(i * THROW_SPACING, () -> {
                if (!registry.owns(session) || session.tasksCancelled() || session.owedReward <= 0
                    || !world.playerOnline()) {
                    return;
                }
                if (!world.salseroValid()) {
                    giveOwed(session, world);
                    return;
                }
                world.dropOne();
                session.owedReward--;
            }));
        }
        // la sesion sigue viva hasta el ultimo tiro: un quit en el medio todavia guarda lo que falta
        session.track(scheduler.later(amount * THROW_SPACING + 1L, () -> {
            if (!registry.owns(session) || session.tasksCancelled()) {
                return;
            }
            if (session.owedReward > 0 && world.playerOnline()) {
                giveOwed(session, world);
            }
            world.release();
        }));
    }

    private static void giveOwed(DonPollosBattleSession session, World world) {
        int owed = session.owedReward;
        session.owedReward = 0;
        world.give(owed);
    }

    /** Suma al jugador lo que la sesion todavia debia (al salir) y deja la sesion en cero. */
    static void storePending(PersistentDataContainer pdc, NamespacedKey key, DonPollosBattleSession session) {
        if (session.owedReward <= 0) {
            return;
        }
        pdc.set(key, PersistentDataType.INTEGER, pdc.getOrDefault(key, PersistentDataType.INTEGER, 0) + session.owedReward);
        session.owedReward = 0;
    }

    /** Lo pendiente del jugador; lo borra, asi se entrega una sola vez. */
    static int claimPending(PersistentDataContainer pdc, NamespacedKey key) {
        int pending = pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
        if (pending > 0) {
            pdc.remove(key);
        }
        return Math.max(0, pending);
    }
}
