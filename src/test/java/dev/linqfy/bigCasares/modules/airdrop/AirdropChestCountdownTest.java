package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AirdropChestCountdownTest {

    @Test
    void formatShowsFiveMinuteLockAfterLanding() {
        assertEquals("§6✈ §dEncantamientos\n§c🔒 Se desbloquea en 05:00",
            AirdropChestCountdown.format(AirdropType.ENCHANT, 0L, 5L * 60_000L, 40L * 60_000L));
    }

    @Test
    void formatShowsRemainingLifetimeAfterUnlock() {
        assertEquals("§6✈ §dEncantamientos\n§a✔ Disponible §7| Desaparece en 34:32",
            AirdropChestCountdown.format(AirdropType.ENCHANT, 5L * 60_000L + 28_000L,
                5L * 60_000L, 40L * 60_000L));
    }

    @Test
    void formatPadsSingleDigitValues() {
        assertEquals("§6✈ §cAlta Explosividad\n§c🔒 Se desbloquea en 03:07",
            AirdropChestCountdown.format(AirdropType.HE, 0L, 3L * 60_000L + 7_000L, 40L * 60_000L));
    }

    @Test
    void formatClampsAtZero() {
        assertEquals("§6✈ §cAlta Explosividad\n§a✔ Disponible §7| Desaparece en 00:00",
            AirdropChestCountdown.format(AirdropType.HE, 50_000L, 10_000L, 40_000L));
    }
}
