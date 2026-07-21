package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperSahurCombatControllerContractTest {

    @Test
    void ownsEverySahurSpecificAbilityEffect() {
        assertEquals(EnumSet.of(
            BossAbilityEffectType.SAHUR_BAT_HIT,
            BossAbilityEffectType.SAHUR_STOMP,
            BossAbilityEffectType.SAHUR_CHARGE,
            BossAbilityEffectType.SAHUR_SPIN,
            BossAbilityEffectType.SAHUR_HUNTER_BAT,
            BossAbilityEffectType.SAHUR_SHIELD_BATS,
            BossAbilityEffectType.SAHUR_LAUNCH_BAT
        ), PaperSahurCombatController.supportedEffects());
    }
}
