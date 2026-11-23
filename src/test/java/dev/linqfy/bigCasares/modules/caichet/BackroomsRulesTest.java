package dev.linqfy.bigCasares.modules.caichet;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Las cuentas de las entidades de los backrooms: sin Bukkit, en ticks y bloques. */
class BackroomsRulesTest {

    /** Un azar que siempre da el mismo numero (para el sorteo del director). */
    private static final class Fixed extends Random {
        private final int value;

        Fixed(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }

    // ------------------------------------------------------------------ director

    @Test
    void theDirectorOnlySchedulesTheFirstTimeAndRollsEveryThirtySeconds() {
        BackroomsRules.Director director = new BackroomsRules.Director();
        UUID id = UUID.randomUUID();
        assertFalse(director.roll(id, 0, true, new Fixed(0)), "la primera vez solo agenda");
        assertFalse(director.roll(id, 599, true, new Fixed(0)));
        assertTrue(director.roll(id, 600, true, new Fixed(0)));
        assertFalse(director.roll(id, 601, true, new Fixed(0)), "el proximo sorteo es 30 s despues");
        assertTrue(director.roll(id, 1200, true, new Fixed(0)));
    }

    @Test
    void theDirectorRollsAThirtyPercentChance() {
        BackroomsRules.Director director = new BackroomsRules.Director();
        UUID id = UUID.randomUUID();
        director.roll(id, 0, true, new Fixed(0));
        assertTrue(director.roll(id, 600, true, new Fixed(29)));
        assertFalse(director.roll(id, 1200, true, new Fixed(30)));
    }

    @Test
    void anIneligiblePlayerLosesTheTurn() {
        BackroomsRules.Director director = new BackroomsRules.Director();
        UUID id = UUID.randomUUID();
        director.roll(id, 0, true, new Fixed(0));
        assertFalse(director.roll(id, 600, false, new Fixed(0)));
        assertFalse(director.roll(id, 601, true, new Fixed(0)), "el turno ya se gasto");
        assertTrue(director.roll(id, 1200, true, new Fixed(0)));
    }

    @Test
    void afterAnEncounterThereAreFortyFiveSecondsWithoutEntities() {
        BackroomsRules.Director director = new BackroomsRules.Director();
        UUID id = UUID.randomUUID();
        director.roll(id, 0, true, new Fixed(0));
        director.ended(id, 100);
        assertTrue(director.isQuiet(id, 100 + 899));
        assertFalse(director.isQuiet(id, 100 + 900));
        assertFalse(director.roll(id, 600, true, new Fixed(0)), "el sorteo cae en el descanso");
        assertTrue(director.roll(id, 1200, true, new Fixed(0)), "y el siguiente ya no");
    }

    @Test
    void theDirectorIsPerPlayerAndForgets() {
        BackroomsRules.Director director = new BackroomsRules.Director();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        director.ended(a, 0);
        assertTrue(director.isQuiet(a, 10));
        assertFalse(director.isQuiet(b, 10));
        director.forget(a);
        assertFalse(director.isQuiet(a, 10));
    }

    // ------------------------------------------------------------------ muerte y techo

    @Test
    void theDeathMessageSurvivesAShortWhileOnly() {
        BackroomsRules.PendingDeaths pending = new BackroomsRules.PendingDeaths();
        UUID id = UUID.randomUUID();
        pending.put(id, BackroomsKind.ARANA, 100);
        assertEquals(BackroomsKind.ARANA, pending.take(id, 112));
        assertNull(pending.take(id, 112), "se usa una sola vez");
        pending.put(id, BackroomsKind.PAYASO, 100);
        assertNull(pending.take(id, 100 + BackroomsRules.PendingDeaths.EXPIRES_TICKS + 1));
    }

    @Test
    void onlySurvivalAndAdventurePlayersDie() {
        assertTrue(BackroomsRules.dies(GameMode.SURVIVAL));
        assertTrue(BackroomsRules.dies(GameMode.ADVENTURE));
        assertFalse(BackroomsRules.dies(GameMode.CREATIVE));
        assertFalse(BackroomsRules.dies(GameMode.SPECTATOR));
        assertEquals(12, BackroomsRules.SCARE_DEATH_AT);
    }

    @Test
    void aFigureShrinksOnlyUnderALowCeiling() {
        assertEquals(1f, BackroomsRules.fit(8f, 2.9f), 1e-6);
        assertEquals(1f, BackroomsRules.fit(3.1f, 2.9f), 1e-6);
        assertEquals((2.5f - 0.2f) / 3.4f, BackroomsRules.fit(2.5f, 3.4f), 1e-6);
        assertEquals(BackroomsRules.MIN_FIT, BackroomsRules.fit(0f, 3.4f), 1e-6, "nunca desaparece");
    }

    // ------------------------------------------------------------------ sonrisa

    @Test
    void theSonrisaLungesAfterThreeSecondsOfBeingLookedAt() {
        BackroomsRules.Sonrisa sonrisa = new BackroomsRules.Sonrisa();
        for (int i = 1; i < 60; i++) {
            assertEquals(BackroomsRules.Sonrisa.Event.NONE, sonrisa.tick(true));
        }
        assertEquals(BackroomsRules.Sonrisa.Event.LUNGE, sonrisa.tick(true));
        assertTrue(sonrisa.lunging());
        assertEquals(BackroomsRules.Sonrisa.Event.NONE, sonrisa.tick(false), "ya no se desvanece");
    }

    @Test
    void theSonrisaVanishesAfterFiveSecondsWithoutBeingLookedAt() {
        BackroomsRules.Sonrisa sonrisa = new BackroomsRules.Sonrisa();
        for (int i = 1; i < 100; i++) {
            assertEquals(BackroomsRules.Sonrisa.Event.NONE, sonrisa.tick(false));
        }
        assertEquals(BackroomsRules.Sonrisa.Event.VANISH, sonrisa.tick(false));
    }

    @Test
    void lookingAtTheSonrisaAgainRestartsTheCountdown() {
        BackroomsRules.Sonrisa sonrisa = new BackroomsRules.Sonrisa();
        for (int i = 0; i < 99; i++) {
            sonrisa.tick(false);
        }
        sonrisa.tick(true);
        assertEquals(0, sonrisa.away());
        assertEquals(1, sonrisa.look());
        for (int i = 1; i < 100; i++) {
            assertEquals(BackroomsRules.Sonrisa.Event.NONE, sonrisa.tick(false));
        }
        assertEquals(BackroomsRules.Sonrisa.Event.VANISH, sonrisa.tick(false));
    }

    @Test
    void theSonrisaLastsAtMostOneMinute() {
        BackroomsRules.Sonrisa sonrisa = new BackroomsRules.Sonrisa();
        BackroomsRules.Sonrisa.Event last = BackroomsRules.Sonrisa.Event.NONE;
        int ticks = 0;
        // se la mira un tick cada 90: ni se lanza (mirada acumulada baja) ni se desvanece (nunca 100 seguidos)
        while (last == BackroomsRules.Sonrisa.Event.NONE && ticks < 5000) {
            ticks++;
            last = sonrisa.tick(ticks % 90 == 0);
        }
        assertEquals(BackroomsRules.Sonrisa.Event.EXPIRE, last);
        assertEquals(BackroomsRules.Sonrisa.MAX_AGE, ticks);
    }

    @Test
    void theLungeFliesEightTenthsOfABlockPerTickAndArrives() {
        assertEquals(9.2, BackroomsRules.Sonrisa.lungeStep(10), 1e-9);
        assertEquals(0, BackroomsRules.Sonrisa.lungeStep(0.5), 1e-9);
        double distance = 14;
        int ticks = 0;
        while (distance > 0) {
            distance = BackroomsRules.Sonrisa.lungeStep(distance);
            ticks++;
        }
        assertEquals(18, ticks);
    }

    // ------------------------------------------------------------------ 666 y araña

    @Test
    void theE666CatchesAtOnePointTwoBlocks() {
        BackroomsRules.Chase e666 = BackroomsRules.Chase.E666;
        assertEquals(BackroomsRules.Chase.Event.NONE, e666.step(1.21, 10));
        assertEquals(BackroomsRules.Chase.Event.CAUGHT, e666.step(1.2, 10));
    }

    @Test
    void theE666GivesUpAfterFortySecondsOrFarAway() {
        BackroomsRules.Chase e666 = BackroomsRules.Chase.E666;
        assertEquals(BackroomsRules.Chase.Event.NONE, e666.step(20, 799));
        assertEquals(BackroomsRules.Chase.Event.GONE, e666.step(20, 800));
        assertEquals(BackroomsRules.Chase.Event.NONE, e666.step(48, 10));
        assertEquals(BackroomsRules.Chase.Event.GONE, e666.step(48.1, 10));
    }

    @Test
    void theAranaIsFasterAndGivesUpAfterFifteenSeconds() {
        BackroomsRules.Chase arana = BackroomsRules.Chase.ARANA;
        assertTrue(arana.speed > BackroomsRules.Chase.E666.speed);
        assertEquals(BackroomsRules.Chase.Event.CAUGHT, arana.step(1.5, 5));
        assertEquals(BackroomsRules.Chase.Event.NONE, arana.step(1.6, 299));
        assertEquals(BackroomsRules.Chase.Event.GONE, arana.step(10, 300));
        assertEquals(BackroomsRules.Chase.Event.GONE, arana.step(50.5, 5));
    }

    @Test
    void aChaserNeverOvershootsThePlayer() {
        assertEquals(0.17, BackroomsRules.Chase.E666.advance(10), 1e-9);
        assertEquals(0.26, BackroomsRules.Chase.ARANA.advance(10), 1e-9);
        assertEquals(0.05, BackroomsRules.Chase.ARANA.advance(0.05), 1e-9);
        assertEquals(0, BackroomsRules.Chase.ARANA.advance(-1), 1e-9);
    }

    @Test
    void theE666FallsBehindAWalkingPlayerAndTheAranaCatchesHim() {
        // caminar son unos 0,216 bloques por tick y correr 0,28
        double gapWalking = 12;
        double gapSprinting = 12;
        for (int i = 0; i < 400; i++) {
            gapWalking += 0.216 - BackroomsRules.Chase.E666.advance(gapWalking);
            gapSprinting += 0.28 - BackroomsRules.Chase.E666.advance(gapSprinting);
        }
        assertTrue(gapWalking > 12 && gapSprinting > gapWalking);
        double gap = 12;
        for (int i = 0; i < 100; i++) {
            gap += 0.216 - BackroomsRules.Chase.ARANA.advance(gap);
        }
        assertTrue(gap < 12);
    }

    @Test
    void theHeartbeatIsFasterWhenItIsClose() {
        assertEquals(36, BackroomsRules.heartbeatEvery(10.5));
        assertEquals(22, BackroomsRules.heartbeatEvery(10));
        assertEquals(CaiChetSounds.E666_HEARTBEAT_SLOW, BackroomsRules.heartbeatSound(20));
        assertEquals(CaiChetSounds.E666_HEARTBEAT_FAST, BackroomsRules.heartbeatSound(3));
    }

    // ------------------------------------------------------------------ juan carlos

    @Test
    void juanCarlosGrabsAtOnePointSixBlocks() {
        BackroomsRules.JuanCarlos jc = new BackroomsRules.JuanCarlos();
        assertEquals(BackroomsRules.JuanCarlos.Event.NONE, jc.tick(1.7));
        assertEquals(BackroomsRules.JuanCarlos.Phase.WALK, jc.phase());
        assertEquals(BackroomsRules.JuanCarlos.Event.GRABBED, jc.tick(1.6));
        assertEquals(BackroomsRules.JuanCarlos.Phase.GRAB, jc.phase());
    }

    @Test
    void juanCarlosLeavesWhenYouAreFarAwayOrAfterFortyFiveSeconds() {
        BackroomsRules.JuanCarlos far = new BackroomsRules.JuanCarlos();
        assertEquals(BackroomsRules.JuanCarlos.Event.NONE, far.tick(40));
        assertEquals(BackroomsRules.JuanCarlos.Event.GONE, far.tick(40.1));
        BackroomsRules.JuanCarlos slow = new BackroomsRules.JuanCarlos();
        for (int i = 1; i < 900; i++) {
            assertEquals(BackroomsRules.JuanCarlos.Event.NONE, slow.tick(20));
        }
        assertEquals(BackroomsRules.JuanCarlos.Event.GONE, slow.tick(20));
    }

    @Test
    void sixClicksInsideTheGrabReleaseYou() {
        BackroomsRules.JuanCarlos jc = new BackroomsRules.JuanCarlos();
        jc.tick(1);
        for (int i = 0; i < 20; i++) {
            jc.tick(1);
        }
        for (int i = 1; i < 6; i++) {
            assertFalse(jc.click());
            assertEquals(i, jc.clicks());
        }
        assertTrue(jc.click(), "el sexto clic te suelta");
        assertEquals(BackroomsRules.JuanCarlos.Phase.RELEASED, jc.phase());
        assertFalse(jc.click(), "ya no cuenta");
        BackroomsRules.JuanCarlos.Event last = BackroomsRules.JuanCarlos.Event.NONE;
        for (int i = 0; i < BackroomsRules.JuanCarlos.RELEASED_FADE_TICKS; i++) {
            last = jc.tick(1);
        }
        assertEquals(BackroomsRules.JuanCarlos.Event.GONE, last, "se desvanece");
    }

    @Test
    void withFewerThanSixClicksHeThrowsYouAndTheScareComesFifteenTicksLater() {
        BackroomsRules.JuanCarlos jc = new BackroomsRules.JuanCarlos();
        assertEquals(BackroomsRules.JuanCarlos.Event.GRABBED, jc.tick(1));
        for (int i = 0; i < 5; i++) {
            jc.click();
        }
        for (int i = 1; i < 60; i++) {
            assertEquals(BackroomsRules.JuanCarlos.Event.NONE, jc.tick(1));
        }
        assertEquals(BackroomsRules.JuanCarlos.Event.THROWN, jc.tick(1));
        for (int i = 1; i < 15; i++) {
            assertEquals(BackroomsRules.JuanCarlos.Event.NONE, jc.tick(1));
        }
        assertEquals(BackroomsRules.JuanCarlos.Event.SCARE, jc.tick(1));
        assertEquals(BackroomsRules.JuanCarlos.Phase.DONE, jc.phase());
        assertEquals(BackroomsRules.JuanCarlos.Event.NONE, jc.tick(1));
    }

    @Test
    void clicksBeforeTheGrabDoNotCount() {
        BackroomsRules.JuanCarlos jc = new BackroomsRules.JuanCarlos();
        for (int i = 0; i < 10; i++) {
            assertFalse(jc.click());
        }
        assertEquals(0, jc.clicks());
    }

    @Test
    void theLiftGoesFromZeroToTwoBlocksButNeverThroughTheCeiling() {
        assertEquals(0, BackroomsRules.JuanCarlos.lift(0, 5), 1e-9);
        assertEquals(1, BackroomsRules.JuanCarlos.lift(30, 5), 1e-9);
        assertEquals(2, BackroomsRules.JuanCarlos.lift(60, 5), 1e-9);
        assertEquals(2, BackroomsRules.JuanCarlos.lift(200, 5), 1e-9);
        assertEquals(0.5, BackroomsRules.JuanCarlos.lift(60, 0.5), 1e-9);
        assertEquals(0, BackroomsRules.JuanCarlos.lift(60, -1), 1e-9);
    }

    // ------------------------------------------------------------------ payaso

    @Test
    void theClownDoesNothingUntilYouSeeHimAndLeavesAfterThirtySeconds() {
        BackroomsRules.Payaso clown = new BackroomsRules.Payaso();
        for (int i = 1; i < 600; i++) {
            assertEquals(BackroomsRules.Payaso.Event.NONE, clown.tick(false, 10));
        }
        assertEquals(BackroomsRules.Payaso.Event.GONE, clown.tick(false, 10));
    }

    @Test
    void seeingTheClownStartsTheFiveSecondDeadline() {
        BackroomsRules.Payaso clown = new BackroomsRules.Payaso();
        assertEquals(BackroomsRules.Payaso.Event.SPOTTED, clown.tick(true, 10));
        assertTrue(clown.spotted());
        assertEquals(100, clown.left());
    }

    @Test
    void movingSixBlocksAwayWins() {
        BackroomsRules.Payaso clown = new BackroomsRules.Payaso();
        clown.tick(true, 10);
        assertEquals(BackroomsRules.Payaso.Event.NONE, clown.tick(false, 15.9));
        assertEquals(BackroomsRules.Payaso.Event.WIN, clown.tick(false, 16));
    }

    @Test
    void gettingThreeBlocksCloserLoses() {
        BackroomsRules.Payaso clown = new BackroomsRules.Payaso();
        clown.tick(true, 10);
        assertEquals(BackroomsRules.Payaso.Event.NONE, clown.tick(false, 7.1));
        assertEquals(BackroomsRules.Payaso.Event.LOSE, clown.tick(false, 7));
    }

    @Test
    void notMovingFarEnoughInFiveSecondsLoses() {
        BackroomsRules.Payaso clown = new BackroomsRules.Payaso();
        clown.tick(true, 10);
        for (int i = 1; i < 100; i++) {
            assertEquals(BackroomsRules.Payaso.Event.NONE, clown.tick(false, 12));
        }
        assertEquals(BackroomsRules.Payaso.Event.LOSE, clown.tick(false, 12));
    }

    @Test
    void theClownDeadlineIsMeasuredFromWhereYouFirstSawHim() {
        BackroomsRules.Payaso clown = new BackroomsRules.Payaso();
        clown.tick(true, 20);
        assertEquals(BackroomsRules.Payaso.Event.NONE, clown.tick(false, 22));
        assertEquals(BackroomsRules.Payaso.Event.WIN, clown.tick(false, 26));
    }
}
