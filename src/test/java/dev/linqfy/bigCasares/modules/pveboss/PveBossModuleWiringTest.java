package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PveBossModuleWiringTest {

    @Test
    void exposesTheStableModuleId() {
        assertEquals("pve-boss-system", new PveBossModule().getId());
        assertEquals(
            List.of("abyss-guardian", "tung-tung-sahur"),
            PveBossModule.supportedBossIds()
        );
    }

    @Test
    void reflectsItsLifecycleWithoutNeedingAServer() {
        PveBossModule module = new PveBossModule();

        module.onEnable();
        assertTrue(module.isEnabled());

        module.onDisable();
        assertFalse(module.isEnabled());
    }

    @Test
    void disablesEveryBossCoordinatorWhenTheModuleStops() {
        PveBossModule module = new PveBossModule();
        BossAbilityCoordinator coordinator = coordinator();
        module.onEnable();
        module.service().registerInstance(coordinator);

        module.onDisable();

        assertTrue(coordinator.isDisabled());
        assertEquals(0, module.service().activeInstanceCount());
    }

    private static BossAbilityCoordinator coordinator() {
        BossAbilityDefinition ability = new BossAbilityDefinition(
            "pulse",
            Duration.ZERO,
            Duration.ZERO,
            BossTargetSelectorDefinition.of(BossTargetSelectorType.NEAREST_PLAYER),
            List.of(),
            List.of(BossAbilityEffectDefinition.of(BossAbilityEffectType.DAMAGE)),
            new BossTelegraphDefinition(BossTelegraphType.CIRCLE, 5, "", ""),
            false,
            0,
            "cast",
            BossBehaviourCategory.PHYSICAL,
            0.0,
            ""
        );
        return new BossAbilityCoordinator(
            UUID.randomUUID(),
            List.of(ability),
            BossAbilityEffectGateway.ignored(),
            new BossTelegraphGateway() {
            }
        );
    }
}
