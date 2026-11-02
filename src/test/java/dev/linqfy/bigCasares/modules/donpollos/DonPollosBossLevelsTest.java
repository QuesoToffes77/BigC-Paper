package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DonPollosBossLevelsTest {

    @Test
    void summonerLevelIsKeptWhetherOnlineOrOffline() {
        // las victorias se leen al poner el bloque: al final del ritual da igual si sigue conectado
        int capturedAtPlacement = 6;
        assertEquals(6, DonPollosBossLevels.levelFor(capturedAtPlacement, List.of()));
        assertEquals(6, DonPollosBossLevels.levelFor(capturedAtPlacement, List.of(6)));
    }

    @Test
    void highestWinsAmongSummonerAndNearbyPlayers() {
        assertEquals(9, DonPollosBossLevels.levelFor(0, List.of(2, 9, 4)));
        assertEquals(6, DonPollosBossLevels.levelFor(6, List.of(2, 0)));
    }

    @Test
    void neverNegative() {
        assertEquals(0, DonPollosBossLevels.levelFor(-3, List.of()));
    }
}
