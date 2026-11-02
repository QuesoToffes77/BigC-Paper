package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static dev.linqfy.bigCasares.modules.donpollos.DonPollosDanceBattle.Outcome;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosDanceTickTest {

    private static final DonPollosBattleSettings SETTINGS =
        new DonPollosBattleSettings(3, 2, 40, 20, 10, new DonPollosItem("DIAMOND", 5), 16, 6, 30);
    private static final Set<DonPollosDanceMove> DIAGONAL =
        Set.of(DonPollosDanceMove.FORWARD, DonPollosDanceMove.LEFT);

    /** Batalla con el paso FORWARD pedido en el tick 0 y vencimiento en 40. */
    private DonPollosDanceBattle forwardAtZero() {
        DonPollosDanceBattle battle = new DonPollosDanceBattle(SETTINGS, bound -> 4, 0, 0);
        assertEquals(Outcome.PROMPT, battle.tick(0));
        assertEquals(DonPollosDanceMove.FORWARD, battle.current());
        return battle;
    }

    @Test
    void pendingDiagonalAtDeadlineMinusOneIsAHit() {
        DonPollosDanceBattle battle = forwardAtZero();
        List<Outcome> outcomes = DonPollosDanceTick.advance(battle, DIAGONAL, 0.5, 0.0, 39);
        assertEquals(List.of(Outcome.HIT, Outcome.NONE), outcomes);
        assertEquals(2, battle.lives());
    }

    @Test
    void pendingDiagonalAtDeadlineAndAfterIsATimeoutCountedOnce() {
        for (long at : new long[]{40, 41}) {
            DonPollosDanceBattle battle = forwardAtZero();
            List<Outcome> outcomes = DonPollosDanceTick.advance(battle, DIAGONAL, 0.5, 0.0, at);
            assertEquals(List.of(Outcome.TIMEOUT, Outcome.NONE), outcomes, "tick " + at);
            assertEquals(1, battle.lives());
        }
    }

    @Test
    void withoutADiagonalOnlyTheClockRuns() {
        DonPollosDanceBattle battle = forwardAtZero();
        assertEquals(List.of(Outcome.NONE), DonPollosDanceTick.advance(battle, null, 0, 0, 39));
        assertEquals(List.of(Outcome.TIMEOUT), DonPollosDanceTick.advance(battle, null, 0, 0, 40));
    }

    @Test
    void inputThenTickAtDeadlineMinusOneHitsAndTickAfterDoesNothing() {
        DonPollosDanceBattle battle = forwardAtZero();
        assertEquals(Outcome.HIT, battle.input(39, Set.of(DonPollosDanceMove.FORWARD)));
        assertEquals(Outcome.NONE, battle.tick(39));
        assertEquals(2, battle.lives());
    }

    @Test
    void tickThenInputAtDeadlineMinusOneHits() {
        DonPollosDanceBattle battle = forwardAtZero();
        assertEquals(Outcome.NONE, battle.tick(39));
        assertEquals(Outcome.HIT, battle.input(39, Set.of(DonPollosDanceMove.FORWARD)));
    }

    @Test
    void engineAnswerAtDeadlineMinusOneIsAccepted() {
        DonPollosDanceBattle battle = forwardAtZero();
        assertEquals(Outcome.HIT, battle.input(39, Set.of(battle.current())));
        assertEquals(2, battle.lives());
    }

    @Test
    void diagonalIsResolvedWhenMovedWaitedOrOneTickLeft() {
        assertTrue(DonPollosDanceTick.shouldResolve(0.1, 0, 10, 10, 30));
        assertTrue(DonPollosDanceTick.shouldResolve(0, 0, 14, 10, 30));
        assertTrue(DonPollosDanceTick.shouldResolve(0, 0, 11, 10, 1));
        assertFalse(DonPollosDanceTick.shouldResolve(0, 0, 11, 10, 2));
    }
}
