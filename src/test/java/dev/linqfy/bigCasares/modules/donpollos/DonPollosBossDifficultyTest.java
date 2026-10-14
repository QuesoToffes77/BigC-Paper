package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosBossDifficultyTest {

    private static final DonPollosBossDifficulty FIRST = new DonPollosBossDifficulty(0);
    private static final DonPollosBossDifficulty AFTER_TWO = new DonPollosBossDifficulty(2);

    @Test
    void theFirstFightIsTheNormalBoss() {
        assertEquals(1.0, FIRST.factor());
        assertEquals(14.0, FIRST.damage(14));
        assertEquals(140, FIRST.cooldownTicks(140));
        assertEquals(2, FIRST.count(2));
        assertEquals(0.23, FIRST.speed(0.23), 1e-9);
        assertEquals(0, new DonPollosBossDifficulty(-3).level());
    }

    @Test
    void eachWinMakesItTwentyFivePercentStronger() {
        assertEquals(1.5, AFTER_TWO.factor());
        assertEquals(21.0, AFTER_TWO.damage(14));
        assertEquals(93, AFTER_TWO.cooldownTicks(140), "ataca mas seguido");
        assertEquals(3, AFTER_TWO.count(2), "invoca mas bichos");
        assertEquals(0.345, AFTER_TWO.speed(0.23), 1e-9, "bichos mas rapidos");
    }

    @Test
    void attacksNeverRepeatFasterThanOncePerSecondAndMobsAtMostTwiceAsFast() {
        DonPollosBossDifficulty veteran = new DonPollosBossDifficulty(40);
        assertEquals(20, veteran.cooldownTicks(140));
        assertEquals(10, veteran.cooldownTicks(10), "lo que ya era mas seguido no se alarga");
        assertEquals(0.46, veteran.speed(0.23), 1e-9);
    }

    @Test
    void cubeRoundsGetMoreLabubusMoreOften() {
        DonPollosCubeRound round = new DonPollosCubeRound(10, 1.0, 1.2, 2);
        DonPollosCubeRound harder = AFTER_TWO.round(round);
        assertEquals(15, harder.labubus());
        assertEquals(1.0 / 1.5, harder.spawnSeconds(), 1e-9);
        assertEquals(round.colonelSpeed(), harder.colonelSpeed());
    }

    @Test
    void theSummonerIsToldHowStrongItIs() {
        assertTrue(DonPollosBossDifficulty.summonMessage(0).contains("nunca"));
        assertEquals("§4Le ganaste §f1§4 vez a Don Pollo: §cesta vez es 25% mas fuerte.",
            DonPollosBossDifficulty.summonMessage(1));
        assertTrue(DonPollosBossDifficulty.summonMessage(4).contains("100% mas fuerte"));
    }
}
