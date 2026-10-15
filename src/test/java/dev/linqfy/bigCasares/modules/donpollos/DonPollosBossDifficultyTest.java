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
    void eachWinMakesItFiftyPercentStronger() {
        assertEquals(1.5, new DonPollosBossDifficulty(1).factor());
        assertEquals(2.0, AFTER_TWO.factor());
        assertEquals(28.0, AFTER_TWO.damage(14));
        assertEquals(70, AFTER_TWO.cooldownTicks(140), "ataca mas seguido");
        assertEquals(4, AFTER_TWO.count(2), "invoca mas bichos");
        assertEquals(0.46, AFTER_TWO.speed(0.23), 1e-9, "bichos mas rapidos");
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
        assertEquals(20, harder.labubus());
        assertEquals(0.5, harder.spawnSeconds(), 1e-9);
        assertEquals(round.colonelSpeed(), harder.colonelSpeed());
    }

    @Test
    void theSummonerIsToldHowStrongItIs() {
        assertTrue(DonPollosBossDifficulty.summonMessage(0).contains("nunca"));
        assertEquals("§4Le ganaste §f1§4 vez a Don Pollo: §cesta vez es 50% mas fuerte.",
            DonPollosBossDifficulty.summonMessage(1));
        assertTrue(DonPollosBossDifficulty.summonMessage(4).contains("200% mas fuerte"));
    }

    @Test
    void spawnCountNeverPassesTheAbsoluteCeilingNoMatterTheDifficulty() {
        // a 100 victorias: tanda 2 -> 102 y tope 8 -> 408, pero el techo absoluto manda
        DonPollosBossDifficulty veteran = new DonPollosBossDifficulty(100);
        assertEquals(24, veteran.spawnCount(2, 8, 24, 0));
        assertEquals(4, veteran.spawnCount(2, 8, 24, 20));
        assertEquals(0, veteran.spawnCount(2, 8, 24, 24));
        assertEquals(0, veteran.spawnCount(2, 8, 24, 40), "nunca negativo");
        // sin dificultad el tope normal sigue valiendo
        assertEquals(2, FIRST.spawnCount(2, 8, 24, 0));
        assertEquals(1, FIRST.spawnCount(2, 8, 24, 7));
        assertEquals(8, new DonPollosBossDifficulty(2).spawnCount(100, 4, 24, 0), "tope escalado 8 < techo 24");
    }
}
