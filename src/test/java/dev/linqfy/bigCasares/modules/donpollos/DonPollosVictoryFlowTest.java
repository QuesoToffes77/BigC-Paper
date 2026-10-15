package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosVictoryFlowTest {

    private static final NamespacedKey PENDING = NamespacedKey.fromString("bigcasares:salsero_premio_pendiente");

    /** Programador de prueba: guarda las tareas y las corre a mano. */
    private static final class FakeScheduler implements DonPollosVictoryFlow.Scheduler {
        final List<Task> tasks = new ArrayList<>();

        final class Task {
            final Runnable body;
            final boolean timer;
            boolean cancelled;

            Task(Runnable body, boolean timer) {
                this.body = body;
                this.timer = timer;
            }
        }

        @Override
        public Runnable later(long delayTicks, Runnable task) {
            Task t = new Task(task, false);
            tasks.add(t);
            return () -> t.cancelled = true;
        }

        @Override
        public Runnable timer(long periodTicks, Runnable task) {
            Task t = new Task(task, true);
            tasks.add(t);
            return () -> t.cancelled = true;
        }

        /** Corre TODAS las tareas registradas hasta ahora, aun las canceladas: simula un callback viejo que ya estaba en cola. */
        void runAllIncludingCancelled() {
            for (Task t : new ArrayList<>(tasks)) {
                t.body.run();
            }
        }

        /** Corre solo las que siguen vivas (como haria Bukkit). */
        void runLive() {
            for (Task t : new ArrayList<>(tasks)) {
                if (!t.cancelled) {
                    t.body.run();
                    if (!t.timer) {
                        t.cancelled = true;
                    }
                }
            }
        }
    }

    private static final class FakeWorld implements DonPollosVictoryFlow.World {
        boolean online = true;
        boolean valid = true;
        boolean close = true;
        int dropped;
        int given;
        int releases;
        int endedWithoutPlayer;
        final DonPollosBattleSession session;
        final DonPollosBattleRegistry<DonPollosBattleSession> registry;
        int restores;

        FakeWorld(DonPollosBattleSession session, DonPollosBattleRegistry<DonPollosBattleSession> registry) {
            this.session = session;
            this.registry = registry;
        }

        public boolean salseroValid() {
            return valid;
        }

        public boolean playerOnline() {
            return online;
        }

        public boolean playerClose() {
            return close;
        }

        public void approach() {
        }

        public void stopAndFace() {
        }

        public void dropOne() {
            dropped++;
        }

        public void give(int amount) {
            given += amount;
        }

        public void endWithoutPlayer() {
            endedWithoutPlayer++;
        }

        /** Igual que el manager: cancela tareas y solo restaura el comportamiento si la sesion era la vigente. */
        public void release() {
            releases++;
            session.cancelTasks();
            if (registry.release(session)) {
                restores++;
            }
        }
    }

    private static PersistentDataContainer fakePdc(Map<NamespacedKey, Object> data) {
        return (PersistentDataContainer) Proxy.newProxyInstance(
            PersistentDataContainer.class.getClassLoader(), new Class<?>[]{PersistentDataContainer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "set" -> {
                    data.put((NamespacedKey) args[0], args[2]);
                    yield null;
                }
                case "get" -> data.get((NamespacedKey) args[0]);
                case "getOrDefault" -> data.getOrDefault((NamespacedKey) args[0], args[2]);
                case "has" -> data.containsKey((NamespacedKey) args[0]);
                case "remove" -> {
                    data.remove((NamespacedKey) args[0]);
                    yield null;
                }
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "fakePdc";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private DonPollosBattleSession session(UUID player, UUID salsero, int owed) {
        DonPollosBattleSession s = new DonPollosBattleSession(player, salsero,
            new DonPollosDanceBattle(DonPollosBattleSettings.defaults(), bound -> 0, 0, 0));
        s.over = true;
        s.owedReward = owed;
        return s;
    }

    @Test
    void staleVictoryCallbacksDoNotTouchANewBattleAndTheRewardIsPaidOnce() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        FakeScheduler scheduler = new FakeScheduler();
        DonPollosVictoryFlow<DonPollosBattleSession> flow = new DonPollosVictoryFlow<>(registry, scheduler);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID salsero = UUID.randomUUID();
        Map<NamespacedKey, Object> pdcA = new HashMap<>();

        // A gana (debe 2) y empieza la caminata
        DonPollosBattleSession sessionA = session(a, salsero, 2);
        assertTrue(registry.register(sessionA));
        FakeWorld worldA = new FakeWorld(sessionA, registry);
        flow.begin(sessionA, worldA);
        assertEquals(1, scheduler.tasks.size());

        // A sale antes de recibir el premio: el manager guarda lo debido, cancela tareas y libera
        sessionA.over = true;
        sessionA.cancelTasks();
        DonPollosVictoryFlow.storePending(fakePdc(pdcA), PENDING, sessionA);
        worldA.release();
        assertEquals(2, pdcA.get(PENDING));
        assertEquals(0, sessionA.owedReward);
        assertEquals(1, worldA.restores);

        // B arranca contra el mismo Salsero
        DonPollosBattleSession sessionB = session(b, salsero, 0);
        sessionB.over = false;
        assertTrue(registry.register(sessionB));
        int releasesBefore = worldA.releases;

        // los callbacks viejos de A corren igual (aun los cancelados): no pasa nada
        scheduler.runAllIncludingCancelled();
        assertEquals(releasesBefore, worldA.releases);
        assertEquals(0, worldA.dropped);
        assertEquals(0, worldA.given);
        assertEquals(0, worldA.endedWithoutPlayer);
        assertSame(sessionB, registry.get(b));
        assertTrue(registry.isBusy(salsero));
        assertTrue(registry.owns(sessionB));
        assertFalse(sessionB.tasksCancelled());
        assertFalse(registry.register(session(UUID.randomUUID(), salsero, 0)));

        // A vuelve: recibe lo pendiente exactamente una vez
        PersistentDataContainer back = fakePdc(pdcA);
        assertEquals(2, DonPollosVictoryFlow.claimPending(back, PENDING));
        assertEquals(0, DonPollosVictoryFlow.claimPending(back, PENDING));
        assertFalse(pdcA.containsKey(PENDING));
    }

    @Test
    void sameUuidReconnectingKeepsItsNewSessionWhenOldCallbacksRun() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        FakeScheduler scheduler = new FakeScheduler();
        DonPollosVictoryFlow<DonPollosBattleSession> flow = new DonPollosVictoryFlow<>(registry, scheduler);
        UUID a = UUID.randomUUID();
        UUID salsero = UUID.randomUUID();

        DonPollosBattleSession old = session(a, salsero, 1);
        registry.register(old);
        FakeWorld oldWorld = new FakeWorld(old, registry);
        flow.begin(old, oldWorld);
        old.cancelTasks();
        registry.release(old);

        DonPollosBattleSession fresh = session(a, UUID.randomUUID(), 0);
        fresh.over = false;
        registry.register(fresh);

        scheduler.runAllIncludingCancelled();
        assertSame(fresh, registry.get(a));
        assertEquals(0, oldWorld.releases);
        assertEquals(0, oldWorld.given + oldWorld.dropped);
    }

    @Test
    void normalWinWalksThrowsEachItemAndReleasesOnce() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        FakeScheduler scheduler = new FakeScheduler();
        DonPollosVictoryFlow<DonPollosBattleSession> flow = new DonPollosVictoryFlow<>(registry, scheduler);
        DonPollosBattleSession s = session(UUID.randomUUID(), UUID.randomUUID(), 2);
        registry.register(s);
        FakeWorld world = new FakeWorld(s, registry);
        flow.begin(s, world);

        scheduler.runLive();  // la caminata llega: programa 2 tiros y el cierre
        assertEquals(0, world.releases);
        scheduler.runLive();  // tiros y cierre
        assertEquals(2, world.dropped);
        assertEquals(0, s.owedReward);
        assertEquals(1, world.releases);
        assertEquals(0, world.given);
        assertFalse(registry.isBattling(s.playerId));
    }

    @Test
    void ownerQuittingMidThrowKeepsWhatIsStillOwed() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        FakeScheduler scheduler = new FakeScheduler();
        DonPollosVictoryFlow<DonPollosBattleSession> flow = new DonPollosVictoryFlow<>(registry, scheduler);
        DonPollosBattleSession s = session(UUID.randomUUID(), UUID.randomUUID(), 2);
        registry.register(s);
        FakeWorld world = new FakeWorld(s, registry);
        flow.begin(s, world);
        scheduler.runLive();  // programa los tiros
        // solo el primer tiro llega a correr
        FakeScheduler.Task first = scheduler.tasks.get(1);
        first.body.run();
        assertEquals(1, world.dropped);
        assertEquals(1, s.owedReward);

        Map<NamespacedKey, Object> pdc = new HashMap<>();
        s.over = true;
        s.cancelTasks();
        DonPollosVictoryFlow.storePending(fakePdc(pdc), PENDING, s);
        world.release();
        scheduler.runAllIncludingCancelled();  // nada mas se entrega
        assertEquals(1, world.dropped);
        assertEquals(0, world.given);
        assertEquals(1, pdc.get(PENDING));
    }
}
