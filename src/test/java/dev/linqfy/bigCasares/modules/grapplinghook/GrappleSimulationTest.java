package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.modules.grapplinghook.GrappleSimulation.GrappleSession;
import dev.linqfy.bigCasares.modules.grapplinghook.GrappleSimulation.GrappleTick;
import dev.linqfy.bigCasares.modules.grapplinghook.GrappleSimulation.Phase;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleSimulationTest {

    private static GrappleSession session(
        UUID owner, Vector start, Vector target, int travelTicks, int attachTicks
    ) {
        return new GrappleSession(owner, GrapplingHookTier.VI, start, target,
            travelTicks, attachTicks, 1.0, 1.1, 0.25, GrapplingHookSettings.MAX_IMPULSE_SPEED, true);
    }

    private static GrappleSession missSession(
        UUID owner, Vector start, Vector target, int travelTicks, int attachTicks
    ) {
        return new GrappleSession(owner, GrapplingHookTier.VI, start, target,
            travelTicks, attachTicks, 1.0, 1.1, 0.25, GrapplingHookSettings.MAX_IMPULSE_SPEED, false);
    }

    private static final Vector ORIGIN = new Vector(0, 0, 0);
    private static final Vector TARGET = new Vector(10, 0, 0);

    @Test
    void hookTravelsAndChainExtendsProgressively() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        int previousChain = 0;
        for (int tick = 1; tick <= 9; tick++) {
            GrappleTick step = session.advance(ORIGIN);
            assertEquals(Phase.TRAVEL, step.phase());
            assertNull(step.impulseVelocity());
            assertTrue(!step.done());
            assertEquals(tick, step.hookPosition().getX(), 1.0e-9);
            assertEquals(tick, step.chainPositions().size());
            assertTrue(step.chainPositions().size() > previousChain,
                "chain must grow while the hook travels");
            previousChain = step.chainPositions().size();
        }
    }

    @Test
    void attachEntryFiresThePullOnceAndTensesTheChain() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        for (int tick = 0; tick < 9; tick++) {
            session.advance(ORIGIN);
        }
        GrappleTick entry = session.advance(ORIGIN); // the hook arrives
        assertEquals(Phase.ATTACH, entry.phase());
        assertEquals(10.0, entry.hookPosition().getX(), 1.0e-9);
        assertEquals(10, entry.chainPositions().size());
        assertNotNull(entry.impulseVelocity(), "the pull fires on the attach-entry tick");
        assertEquals(1.1, entry.impulseVelocity().getX(), 1.0e-9);
        assertEquals(0.25, entry.impulseVelocity().getY(), 1.0e-9);

        // The chain stays tense while swinging; the pull fires exactly once.
        GrappleTick swing = session.advance(new Vector(2, 0, 0));
        assertEquals(Phase.ATTACH, swing.phase());
        assertNull(swing.impulseVelocity());
        assertEquals(10.0, swing.hookPosition().getX(), 1.0e-9);
        Vector chainEnd = swing.chainPositions().get(swing.chainPositions().size() - 1);
        assertEquals(swing.hookPosition().distance(chainEnd), 0.0, 1.0e-6);
    }

    @Test
    void swingEndsWhenThePlayerArrives() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        for (int tick = 0; tick < 9; tick++) {
            session.advance(ORIGIN);
        }
        GrappleTick entry = session.advance(ORIGIN);
        assertEquals(Phase.ATTACH, entry.phase());
        assertNotNull(entry.impulseVelocity());

        // The player flies toward the target; while far the chain stays tense.
        for (int tick = 0; tick < 4; tick++) {
            GrappleTick swing = session.advance(new Vector(2, 0, 0));
            assertEquals(Phase.ATTACH, swing.phase());
            assertNull(swing.impulseVelocity());
        }
        // Once the anchor is within arrival distance the swing ends.
        GrappleTick done = session.advance(new Vector(8.2, 0, 0));
        assertTrue(done.done());
        assertEquals(Phase.DONE, done.phase());
        assertNull(done.impulseVelocity());
        assertThrows(IllegalStateException.class, () -> session.advance(ORIGIN));
    }

    @Test
    void missSessionsNeverPullAndEndAfterTheHold() {
        GrappleSession session = missSession(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        for (int tick = 0; tick < 9; tick++) {
            assertEquals(Phase.TRAVEL, session.advance(ORIGIN).phase());
        }
        GrappleTick entry = session.advance(ORIGIN);
        assertEquals(Phase.ATTACH, entry.phase());
        assertNull(entry.impulseVelocity(), "a miss never pulls");
        for (int tick = 0; tick < 3; tick++) {
            assertEquals(Phase.ATTACH, session.advance(ORIGIN).phase());
        }
        GrappleTick done = session.advance(ORIGIN);
        assertTrue(done.done());
        assertNull(done.impulseVelocity());
    }

    @Test
    void alreadyCloseTargetHoldsBrieflyAndEndsWithoutAbsurdSpeed() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, new Vector(1.5, 0, 0), 2, 4);
        assertEquals(Phase.TRAVEL, session.advance(ORIGIN).phase());
        GrappleTick attach = session.advance(ORIGIN); // attach entry
        assertEquals(Phase.ATTACH, attach.phase());
        assertNotNull(attach.impulseVelocity());
        // The velocity stays under the hard ceiling even at point-blank range.
        assertTrue(attach.impulseVelocity().length() <= GrapplingHookSettings.MAX_IMPULSE_SPEED + 1.0e-9);

        for (int tick = 0; tick < 3; tick++) {
            assertEquals(Phase.ATTACH, session.advance(ORIGIN).phase());
        }
        GrappleTick done = session.advance(ORIGIN);
        assertTrue(done.done());
    }

    @Test
    void neverArrivingSwingEndsAtTheHardCap() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        int ticks = 0;
        GrappleTick step;
        do {
            step = session.advance(ORIGIN);
            ticks++;
        } while (!step.done());
        assertEquals(10 + GrappleSimulation.MAX_ATTACH_TICKS, ticks);
        assertNull(step.impulseVelocity());
    }

    @Test
    void retractDuringTravelCancelsTheShot() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        for (int tick = 0; tick < 3; tick++) {
            session.advance(ORIGIN);
        }
        assertEquals(Phase.TRAVEL, session.phase());
        session.beginRetract();
        assertEquals(Phase.RETRACT, session.phase());

        GrappleTick first = session.advance(ORIGIN);
        assertEquals(Phase.RETRACT, first.phase());
        assertNull(first.impulseVelocity());
        assertTrue(first.hookPosition().distance(ORIGIN) < 10.0, "hook must return to the player");

        // A repeated activation while retracting is ignored: no second session.
        session.beginRetract();
        assertEquals(Phase.RETRACT, session.phase());
    }

    @Test
    void retractDuringAttachReturnsHookAndShrinksChain() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        for (int tick = 0; tick < 10; tick++) {
            session.advance(ORIGIN);
        }
        assertEquals(Phase.ATTACH, session.phase());
        session.beginRetract();

        // retractTicks == travelTicks: 9 returning ticks, then the final one
        // is the DONE tick.
        int previousChain = Integer.MAX_VALUE;
        GrappleTick step = null;
        for (int tick = 0; tick < 9; tick++) {
            step = session.advance(ORIGIN);
            assertEquals(Phase.RETRACT, step.phase());
            assertNull(step.impulseVelocity());
            assertTrue(step.chainPositions().size() <= previousChain,
                "the chain must disappear progressively while retracting");
            previousChain = step.chainPositions().size();
        }
        assertNotNull(step);
        assertTrue(step.hookPosition().distance(ORIGIN) <= 1.0 + 1.0e-9);

        GrappleTick done = session.advance(ORIGIN);
        assertTrue(done.done());
        assertEquals(Phase.DONE, done.phase());
        assertNull(done.impulseVelocity());
        assertTrue(done.chainPositions().isEmpty(), "retract ends with no chain left");
        assertThrows(IllegalStateException.class, () -> session.advance(ORIGIN));
    }

    @Test
    void configuredFastRetractCollectsTheRopeTwiceAsFast() {
        GrappleSession session = new GrappleSession(UUID.randomUUID(), GrapplingHookTier.VI,
            ORIGIN, TARGET, 10, 4, 1.0, 1.1, 0.25,
            GrapplingHookSettings.MAX_IMPULSE_SPEED, true, 2.0);
        session.advance(ORIGIN);
        session.beginRetract();

        for (int tick = 0; tick < 4; tick++) {
            assertEquals(Phase.RETRACT, session.advance(ORIGIN).phase());
        }
        assertTrue(session.advance(ORIGIN).done());
    }

    @Test
    void chainFollowsMovingAnchor() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        GrappleTick step = session.advance(new Vector(0, 0, 1)); // player strafed sideways
        Vector first = step.chainPositions().get(0);
        assertEquals(1.0, new Vector(0, 0, 1).distance(first), 1.0e-6);
        Vector last = step.chainPositions().get(step.chainPositions().size() - 1);
        assertEquals(step.hookPosition().distance(last), 0.0, 1.0e-6);
    }

    @Test
    void liveTargetMovesTheHookDuringTravel() {
        // Entity target walks sideways mid-flight: the hook must track it.
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        GrappleTick first = session.advance(ORIGIN, new Vector(10, 0, 5));
        assertEquals(1.0, first.hookPosition().getX(), 1.0e-9);
        assertEquals(0.5, first.hookPosition().getZ(), 1.0e-9);

        for (int tick = 0; tick < 8; tick++) {
            session.advance(ORIGIN, new Vector(10, 0, 5));
        }
        GrappleTick reached = session.advance(ORIGIN, new Vector(10, 0, 5)); // travel completes
        assertEquals(Phase.ATTACH, reached.phase());
        assertEquals(10.0, reached.hookPosition().getX(), 1.0e-9);
        assertEquals(5.0, reached.hookPosition().getZ(), 1.0e-9);
    }

    @Test
    void attachAndTenseChainFollowTheLiveTarget() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        for (int tick = 0; tick < 9; tick++) {
            session.advance(ORIGIN, new Vector(10, 0, 0));
        }
        GrappleTick entry = session.advance(ORIGIN, new Vector(10, 0, 0)); // attach entry
        assertEquals(Phase.ATTACH, entry.phase());
        assertEquals(10.0, entry.hookPosition().getX(), 1.0e-9);
        assertNotNull(entry.impulseVelocity());

        // The entity moves to (10, 0, 8): hook and tense chain follow it.
        GrappleTick moved = session.advance(ORIGIN, new Vector(10, 0, 8));
        assertEquals(Phase.ATTACH, moved.phase());
        assertEquals(8.0, moved.hookPosition().getZ(), 1.0e-9);
        Vector last = moved.chainPositions().get(moved.chainPositions().size() - 1);
        assertEquals(8.0, last.getZ(), 1.0e-9);
        assertNull(moved.impulseVelocity());
    }

    @Test
    void sessionsAreIndependentPerPlayer() {
        GrappleSession first = session(UUID.randomUUID(), ORIGIN, TARGET, 4, 1);
        GrappleSession second = session(UUID.randomUUID(), ORIGIN, TARGET, 8, 1);

        for (int tick = 0; tick < 4; tick++) {
            first.advance(ORIGIN);
            second.advance(ORIGIN);
        }
        // First reached the attach entry (hook on target); it stays attached
        // while the second is still travelling.
        GrappleTick firstTick = first.advance(ORIGIN);
        assertEquals(Phase.ATTACH, firstTick.phase());
        assertEquals(10.0, firstTick.hookPosition().getX(), 1.0e-9);
        GrappleTick secondTick = second.advance(ORIGIN);
        assertEquals(Phase.TRAVEL, secondTick.phase());
        assertEquals(6.25, secondTick.hookPosition().getX(), 1.0e-9);
    }

    @Test
    void advanceWithSingleArgumentStillUsesTheCapturedTarget() {
        GrappleSession session = session(UUID.randomUUID(), ORIGIN, TARGET, 10, 4);
        GrappleTick step = session.advance(ORIGIN);
        assertEquals(1.0, step.hookPosition().getX(), 1.0e-9);
        assertEquals(0.0, step.hookPosition().getZ(), 1.0e-9);
    }

    @Test
    void freshSessionStartsCleanAfterAnotherFinishes() {
        GrappleSession finished = missSession(UUID.randomUUID(), ORIGIN, TARGET, 4, 1);
        GrappleTick step;
        int ticks = 0;
        do {
            step = finished.advance(ORIGIN);
            ticks++;
        } while (!step.done());
        assertTrue(step.done());
        assertTrue(ticks <= 6, "a miss session must end quickly, took " + ticks + " ticks");

        GrappleSession fresh = session(UUID.randomUUID(), ORIGIN, TARGET, 4, 1);
        GrappleTick freshStep = fresh.advance(ORIGIN);
        assertEquals(Phase.TRAVEL, freshStep.phase());
        assertEquals(2.5, freshStep.hookPosition().getX(), 1.0e-9);
    }
}
