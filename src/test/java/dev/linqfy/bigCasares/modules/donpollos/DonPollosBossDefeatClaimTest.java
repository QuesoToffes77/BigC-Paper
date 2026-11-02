package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reclamar el premio del boss derrotado: solo el ganador, aunque este desconectado, y una sola vez. */
class DonPollosBossDefeatClaimTest {

    private final UUID winner = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void theWinnerClaimsOnce() {
        DonPollosBoss.Defeat defeat = new DonPollosBoss.Defeat(winner, 0);
        assertTrue(defeat.tryClaim(winner));
        assertFalse(defeat.tryClaim(winner), "no se cobra dos veces");
        assertTrue(defeat.claimed);
    }

    @Test
    void someoneElseCannotClaimAndDoesNotUseItUp() {
        DonPollosBoss.Defeat defeat = new DonPollosBoss.Defeat(winner, 0);
        assertFalse(defeat.tryClaim(other));
        assertFalse(defeat.claimed);
        assertTrue(defeat.tryClaim(winner), "el ganador lo sigue teniendo");
    }

    @Test
    void withoutAWinnerTheFirstOneTakesIt() {
        DonPollosBoss.Defeat defeat = new DonPollosBoss.Defeat(null, 0);
        assertTrue(defeat.tryClaim(other));
        assertFalse(defeat.tryClaim(winner));
    }
}
