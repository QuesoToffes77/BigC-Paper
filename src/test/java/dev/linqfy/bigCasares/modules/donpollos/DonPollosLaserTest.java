package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosLaserTest {

    @Test
    void whoeverTheBeamTouchesTakesTheHit() {
        // el objetivo era "objetivo", pero se cruzo "amigo": el golpe es para el amigo
        assertEquals(Set.of("amigo"), DonPollosLaser.victims(List.of("amigo", "amigo")));
    }

    @Test
    void bothEyesOnTheSamePlayerCountOnce() {
        assertEquals(1, DonPollosLaser.victims(List.of("objetivo", "objetivo")).size());
    }

    @Test
    void eachEyeCanHitADifferentPlayer() {
        assertEquals(Set.of("uno", "dos"), DonPollosLaser.victims(List.of("uno", "dos")));
    }

    @Test
    void beamsStoppedByAWallHitNobody() {
        assertTrue(DonPollosLaser.victims(Arrays.asList(null, null)).isEmpty());
        assertEquals(Set.of("uno"), DonPollosLaser.victims(Arrays.asList(null, "uno")));
    }
}
