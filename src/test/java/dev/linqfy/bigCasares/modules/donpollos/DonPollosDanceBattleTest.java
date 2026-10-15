package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosDanceBattleTest {

    private static final DonPollosBattleSettings SETTINGS =
        new DonPollosBattleSettings(3, 2, 40, 20, 10, new DonPollosItem("DIAMOND", 5), 16, 6, 30);

    @Test
    void firstStepComesAfterTheIntro() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 30);

        assertEquals(DonPollosDanceBattle.Outcome.NONE, battle.tick(29));
        assertEquals(DonPollosDanceBattle.Outcome.PROMPT, battle.tick(30));
        assertEquals(DonPollosDanceMove.JUMP, battle.current());
    }

    @Test
    void doingTheRightStepTakesSalseroHealthAndAllStepsWin() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        long now = 0;
        for (int hit = 1; hit <= 3; hit++) {
            assertEquals(DonPollosDanceBattle.Outcome.PROMPT, battle.tick(now));
            DonPollosDanceBattle.Outcome outcome = battle.input(now + 1, Set.of(battle.current()));
            assertEquals(hit == 3 ? DonPollosDanceBattle.Outcome.WON : DonPollosDanceBattle.Outcome.HIT, outcome);
            assertEquals((3 - hit) / 3f, battle.salseroHealth(), 1e-6);
            now += 1 + SETTINGS.pauseTicks();
        }
        assertTrue(battle.isWon());
    }

    @Test
    void wrongKeyOrTimeoutCostsALifeAndLosingAllLivesLoses() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);

        battle.tick(0);
        assertEquals(DonPollosDanceBattle.Outcome.MISS, battle.input(5, Set.of(DonPollosDanceMove.LEFT)));
        assertEquals(1, battle.lives());
        assertNull(battle.current());

        battle.tick(5 + 2L * SETTINGS.pauseTicks());
        long deadline = 5 + 2L * SETTINGS.pauseTicks() + battle.windowTicks();
        assertEquals(DonPollosDanceBattle.Outcome.NONE, battle.tick(deadline - 1));
        assertEquals(DonPollosDanceBattle.Outcome.LOST, battle.tick(deadline));
        assertTrue(battle.isLost());
    }

    @Test
    void onlyTheRequestedKeyAloneCountsAsAHit() {
        // con bound -> 0 el primer paso pedido es siempre JUMP
        DonPollosDanceBattle alone = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        alone.tick(0);
        assertEquals(DonPollosDanceMove.JUMP, alone.current());
        assertEquals(DonPollosDanceBattle.Outcome.HIT, alone.input(1, Set.of(DonPollosDanceMove.JUMP)));

        DonPollosDanceBattle wrong = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        wrong.tick(0);
        assertEquals(DonPollosDanceBattle.Outcome.MISS, wrong.input(1, Set.of(DonPollosDanceMove.SNEAK)));

        DonPollosDanceBattle withAnother = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        withAnother.tick(0);
        assertEquals(DonPollosDanceBattle.Outcome.MISS,
            withAnother.input(1, Set.of(DonPollosDanceMove.JUMP, DonPollosDanceMove.SNEAK)));
        assertEquals(1, withAnother.lives());
        assertEquals(3, withAnother.hitsLeft());

        DonPollosDanceBattle everything = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        everything.tick(0);
        assertEquals(DonPollosDanceBattle.Outcome.MISS, everything.input(1, Set.of(DonPollosDanceMove.values())));
        assertEquals(3, everything.hitsLeft());
    }

    @Test
    void mashingEveryKeyNeverWinsTheBattle() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        for (long now = 0; !battle.isOver() && now < 10_000; now++) {
            if (battle.tick(now) == DonPollosDanceBattle.Outcome.PROMPT) {
                battle.input(now + 1, Set.of(DonPollosDanceMove.values()));
            }
        }
        assertTrue(battle.isLost());
    }

    @Test
    void inputWithoutAStepPendingIsIgnored() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 100);

        assertEquals(DonPollosDanceBattle.Outcome.NONE, battle.input(1, Set.of(DonPollosDanceMove.JUMP)));
        assertEquals(2, battle.lives());
    }

    @Test
    void windowShrinksAsTheSalseroLosesHealth() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        assertEquals(40, battle.windowTicks());
        battle.tick(0);
        battle.input(1, Set.of(battle.current()));
        battle.tick(100);
        battle.input(101, Set.of(battle.current()));
        assertEquals(27, battle.windowTicks());
    }

    @Test
    void neverAsksTheSameStepTwiceInARow() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        battle.tick(0);
        DonPollosDanceMove first = battle.current();
        battle.input(1, Set.of(first));
        battle.tick(100);
        assertNotEquals(first, battle.current());
    }

    @Test
    void reactionTimeIsDividedByPreviousWins() {
        // SETTINGS: 40 ticks al empezar
        assertEquals(40, new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 0).windowTicks());
        assertEquals(40, new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 1).windowTicks());
        assertEquals(20, new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 2).windowTicks());
        assertEquals(10, new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 4).windowTicks());
        assertEquals(SETTINGS.minHumanWindowTicks(),
            new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 1000).windowTicks());
        assertEquals(20, DonPollosDanceBattle.startWindowTicks(SETTINGS, 2));
    }

    @Test
    void theShrinkingWindowIsAlsoDividedByPreviousWins() {
        DonPollosDanceBattle fresh = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 0);
        DonPollosDanceBattle veteran = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0, 3);
        for (DonPollosDanceBattle battle : new DonPollosDanceBattle[]{fresh, veteran}) {
            battle.tick(0);
            battle.input(1, Set.of(battle.current()));
            battle.tick(100);
            battle.input(101, Set.of(battle.current()));
        }
        assertEquals(27, fresh.windowTicks());
        assertEquals(9, veteran.windowTicks());
    }

    @Test
    void salseroPaysTwoDiamondsByDefault() {
        assertEquals(new DonPollosItem("DIAMOND", 2), DonPollosBattleSettings.defaults().reward());
    }

    @Test
    void onlyTheFirstTwoWinsPayTheFullReward() {
        DonPollosBattleSettings settings = DonPollosBattleSettings.defaults();
        assertEquals(2, settings.rewardAmount(1));
        assertEquals(2, settings.rewardAmount(2));
        assertEquals(1, settings.rewardAmount(3));
        assertEquals(1, settings.rewardAmount(50));
    }

    @Test
    void windowNeverGoesBelowTheHumanFloor() {
        DonPollosBattleSettings defaults = DonPollosBattleSettings.defaults();
        assertEquals(6, defaults.minHumanWindowTicks());
        for (int wins : new int[]{10, 20, 50, 1000}) {
            assertEquals(6, DonPollosDanceBattle.startWindowTicks(defaults, wins));
            DonPollosDanceBattle battle = new DonPollosDanceBattle(defaults, bound -> 0, 0, 0, wins);
            assertEquals(6, battle.windowTicks());
        }
        // sin llegar al piso se sigue dividiendo por las victorias previas
        assertEquals(25, DonPollosDanceBattle.startWindowTicks(defaults, 2));
        assertEquals(10, DonPollosDanceBattle.startWindowTicks(defaults, 5));
        // el piso es configurable
        DonPollosBattleSettings strict = new DonPollosBattleSettings(3, 2, 40, 20, 10,
            new DonPollosItem("DIAMOND", 5), 16, 12, 30);
        assertEquals(12, DonPollosDanceBattle.startWindowTicks(strict, 100));
    }

    @Test
    void answerAtOrAfterTheDeadlineIsLateEvenIfTickHasNotRun() {
        long deadline = 40;  // prompt en 0, ventana 40
        for (long at : new long[]{deadline, deadline + 1}) {
            DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
            battle.tick(0);
            assertEquals(DonPollosDanceBattle.Outcome.TIMEOUT, battle.input(at, Set.of(battle.current())));
            assertEquals(1, battle.lives());
            assertEquals(3, battle.hitsLeft());
            assertNull(battle.current());
        }
        DonPollosDanceBattle early = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        early.tick(0);
        assertEquals(DonPollosDanceBattle.Outcome.HIT, early.input(deadline - 1, Set.of(early.current())));
    }

    @Test
    void inputThenTickAndTickThenInputAgreeOnTheDeadline() {
        // input primero: pierde la vida; el tick siguiente ya no tiene un paso que vencer (no cuenta doble)
        DonPollosDanceBattle inputFirst = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        inputFirst.tick(0);
        assertEquals(DonPollosDanceBattle.Outcome.TIMEOUT, inputFirst.input(40, Set.of(inputFirst.current())));
        assertEquals(DonPollosDanceBattle.Outcome.NONE, inputFirst.tick(40));
        assertEquals(1, inputFirst.lives());

        // tick primero: vence; la respuesta tardia no encuentra paso y se ignora
        DonPollosDanceBattle tickFirst = new DonPollosDanceBattle(SETTINGS, bound -> 0, 0, 0);
        tickFirst.tick(0);
        DonPollosDanceMove asked = tickFirst.current();
        assertEquals(DonPollosDanceBattle.Outcome.TIMEOUT, tickFirst.tick(40));
        assertEquals(DonPollosDanceBattle.Outcome.NONE, tickFirst.input(40, Set.of(asked)));
        assertEquals(1, tickFirst.lives());
    }

    @Test
    void pendingJoystickDiagonalResolvedAtOrAfterTheDeadlineCountsAsTimeout() {
        Set<DonPollosDanceMove> diagonal = Set.of(DonPollosDanceMove.FORWARD, DonPollosDanceMove.LEFT);
        // el jugador se movio hacia adelante: la diagonal se resuelve como FORWARD
        DonPollosDanceMove resolved = DonPollosJoystick.dominant(diagonal, 0.5, 0.1);
        assertEquals(DonPollosDanceMove.FORWARD, resolved);

        // paso pedido FORWARD (indice 4), ventana 40: diagonal apretada en 38, el manager la resuelve despues
        for (long resolvedAt : new long[]{39, 40, 42}) {
            DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 4, 0, 0);
            battle.tick(0);
            assertEquals(DonPollosDanceMove.FORWARD, battle.current());
            battle.tick(38);  // la diagonal quedo pendiente
            // en el tick de resolucion el manager llama input() antes de tick()
            DonPollosDanceBattle.Outcome outcome = battle.input(resolvedAt, Set.of(resolved));
            assertEquals(resolvedAt < 40 ? DonPollosDanceBattle.Outcome.HIT : DonPollosDanceBattle.Outcome.TIMEOUT,
                outcome);
            if (resolvedAt >= 40) {
                assertEquals(DonPollosDanceBattle.Outcome.NONE, battle.tick(resolvedAt));
                assertEquals(1, battle.lives());
            }
        }
    }

    @Test
    void rewardCooldownDecision() {
        DonPollosBattleSettings s = DonPollosBattleSettings.defaults();  // 30 minutos
        long now = 10_000_000_000L;
        assertEquals(0, s.rewardCooldownRemainingMinutes(0, now));  // nunca cobro
        assertEquals(30, s.rewardCooldownRemainingMinutes(now, now));
        assertEquals(30, s.rewardCooldownRemainingMinutes(now - 1, now));  // redondea hacia arriba
        assertEquals(1, s.rewardCooldownRemainingMinutes(now - 29 * 60_000L - 1, now));
        assertEquals(0, s.rewardCooldownRemainingMinutes(now - 30 * 60_000L, now));
        assertEquals(0, s.rewardCooldownRemainingMinutes(now - 31 * 60_000L, now));
        assertEquals(30, s.rewardCooldownRemainingMinutes(now + 5_000, now));  // reloj atrasado
        DonPollosBattleSettings off = new DonPollosBattleSettings(3, 2, 40, 20, 10,
            new DonPollosItem("DIAMOND", 5), 16, 6, 0);
        assertEquals(0, off.rewardCooldownRemainingMinutes(now, now));
    }

    @Test
    void humanFloorCannotExceedTheStartWindow() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> new DonPollosBattleSettings(3, 2, 40, 20, 10, new DonPollosItem("DIAMOND", 5), 16, 41, 30));
    }
}
