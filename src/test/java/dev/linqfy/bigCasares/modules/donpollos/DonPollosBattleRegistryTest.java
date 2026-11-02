package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosBattleRegistryTest {

    private final DonPollosBattleSettings settings = DonPollosBattleSettings.defaults();

    private DonPollosBattleSession session(UUID player, UUID salsero) {
        return new DonPollosBattleSession(player, salsero,
            new DonPollosDanceBattle(settings, bound -> 0, 0, 0));
    }

    /** Lo que hace el manager al salir: guardar lo debido, terminar y liberar. Devuelve lo guardado como pendiente. */
    private int quit(DonPollosBattleRegistry<DonPollosBattleSession> registry, DonPollosBattleSession session) {
        session.over = true;
        session.cancelTasks();
        int stored = session.owedReward;
        session.owedReward = 0;
        registry.release(session);
        return stored;
    }

    @Test
    void oldVictoryCallbackCannotReleaseANewBattleOfTheSameSalsero() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID salsero = UUID.randomUUID();

        DonPollosBattleSession sessionA = session(a, salsero);
        assertTrue(registry.register(sessionA));
        // A gana: el premio queda debido y hay una tarea de caminata pendiente
        sessionA.owedReward = 2;
        List<String> log = new ArrayList<>();
        boolean[] walkRan = {false};
        sessionA.track(() -> log.add("walk cancelled"));

        // A sale antes de recibirlo: se guarda lo debido, se cancelan las tareas y se libera al Salsero
        assertEquals(2, quit(registry, sessionA));
        assertEquals(List.of("walk cancelled"), log);
        assertFalse(registry.isBusy(salsero));

        // B empieza otra batalla con el mismo Salsero
        DonPollosBattleSession sessionB = session(b, salsero);
        assertTrue(registry.register(sessionB));

        // el callback viejo de A igual corre: no es dueno de nada
        assertFalse(registry.owns(sessionA));
        walkRan[0] = registry.release(sessionA);
        assertFalse(walkRan[0]);

        assertTrue(registry.isBusy(salsero));
        assertTrue(registry.isBattling(b));
        assertSame(sessionB, registry.get(b));
        assertTrue(registry.owns(sessionB));
        // y C no puede abrir una batalla simultanea con el mismo Salsero
        assertFalse(registry.register(session(UUID.randomUUID(), salsero)));
    }

    @Test
    void reconnectedPlayerKeepsTheNewSession() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        UUID a = UUID.randomUUID();
        UUID salsero = UUID.randomUUID();
        UUID otherSalsero = UUID.randomUUID();

        DonPollosBattleSession first = session(a, salsero);
        registry.register(first);
        quit(registry, first);

        DonPollosBattleSession second = session(a, otherSalsero);
        assertTrue(registry.register(second));

        assertFalse(registry.release(first));  // callback viejo del mismo UUID
        assertSame(second, registry.get(a));
        assertTrue(registry.isBusy(otherSalsero));
        assertTrue(registry.release(second));
        assertFalse(registry.isBattling(a));
        assertFalse(registry.isBusy(otherSalsero));
    }

    @Test
    void sessionReplacedOnTheSameSalseroWithDifferentPlayerIsNotReleasedByTheOldOne() {
        DonPollosBattleRegistry<DonPollosBattleSession> registry = new DonPollosBattleRegistry<>();
        UUID salsero = UUID.randomUUID();
        DonPollosBattleSession old = session(UUID.randomUUID(), salsero);
        registry.register(old);
        assertTrue(registry.release(old));
        assertFalse(registry.release(old));  // doble liberacion: no hace nada
        DonPollosBattleSession fresh = session(UUID.randomUUID(), salsero);
        registry.register(fresh);
        assertFalse(registry.release(old));
        assertTrue(registry.owns(fresh));
    }

    @Test
    void cancelledSessionCancelsLateRegisteredTasksImmediately() {
        DonPollosBattleSession s = session(UUID.randomUUID(), UUID.randomUUID());
        int[] cancelled = {0};
        s.track(() -> cancelled[0]++);
        s.cancelTasks();
        assertEquals(1, cancelled[0]);
        s.cancelTasks();  // no repite
        assertEquals(1, cancelled[0]);
        s.track(() -> cancelled[0]++);
        assertEquals(2, cancelled[0]);
        assertTrue(s.tasksCancelled());
    }
}
