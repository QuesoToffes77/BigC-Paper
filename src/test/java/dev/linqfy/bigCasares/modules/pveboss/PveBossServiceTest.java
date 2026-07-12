package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PveBossServiceTest {

    @Test
    void allowsExactlyOneCoordinatorPerBossInstance() {
        PveBossService service = new PveBossService();
        UUID bossId = UUID.randomUUID();
        BossAbilityCoordinator first = coordinator(bossId);
        BossAbilityCoordinator duplicate = coordinator(bossId);

        service.registerInstance(first);

        assertThrows(IllegalStateException.class, () -> service.registerInstance(duplicate));
        assertEquals(first, service.coordinator(bossId).orElseThrow());
    }

    @Test
    void cancelsAndRemovesEveryCoordinatorAtShutdown() {
        PveBossService service = new PveBossService();
        BossAbilityCoordinator first = coordinator(UUID.randomUUID());
        BossAbilityCoordinator second = coordinator(UUID.randomUUID());
        service.registerInstance(first);
        service.registerInstance(second);

        service.shutdown(Instant.parse("2026-07-11T00:00:00Z"));

        assertTrue(first.isDisabled());
        assertTrue(second.isDisabled());
        assertEquals(0, service.activeInstanceCount());
    }

    private static BossAbilityCoordinator coordinator(UUID bossId) {
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
            bossId,
            List.of(ability),
            BossAbilityEffectGateway.ignored(),
            new BossTelegraphGateway() {
            }
        );
    }
}
