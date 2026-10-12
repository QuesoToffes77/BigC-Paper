package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosDanceBattleTest {

    private static final DonPollosBattleSettings SETTINGS =
        new DonPollosBattleSettings(3, 2, 40, 20, 10, new DonPollosItem("DIAMOND", 5), 16);

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
}
