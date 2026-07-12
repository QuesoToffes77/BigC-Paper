package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossAbilityCoordinatorTest {

    @Test
    void selectsTheHighestPriorityAvailableAbilityAndNeverCastsTwoAtOnce() {
        UUID bossId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        RecordingEffects effects = new RecordingEffects();
        BossAbilityCoordinator coordinator = coordinator(
            bossId,
            List.of(ability("low", 10, Duration.ofSeconds(1), Duration.ZERO, true),
                ability("high", 50, Duration.ofSeconds(2), Duration.ZERO, true)),
            effects,
            new RecordingTelegraphs()
        );
        Instant start = Instant.parse("2026-07-11T00:00:00Z");

        BossAbilityRuntime firstTick = coordinator.tick(start, context(targetId)).orElseThrow();
        BossAbilityRuntime secondTick = coordinator.tick(start.plusMillis(500), context(targetId)).orElseThrow();

        assertEquals("high", firstTick.abilityId());
        assertEquals(BossAbilityState.CASTING, firstTick.state());
        assertEquals("high", secondTick.abilityId());
        assertEquals(BossAbilityState.CASTING, secondTick.state());
        assertTrue(effects.executions.isEmpty());
    }

    @Test
    void completesACastOnceAndKeepsTheAbilityOnCooldown() {
        UUID bossId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        RecordingEffects effects = new RecordingEffects();
        BossAbilityCoordinator coordinator = coordinator(
            bossId,
            List.of(ability("pulse", 50, Duration.ofSeconds(1), Duration.ofSeconds(10), true)),
            effects,
            new RecordingTelegraphs()
        );
        Instant start = Instant.parse("2026-07-11T00:00:00Z");

        coordinator.tick(start, context(targetId));
        BossAbilityRuntime completed = coordinator.tick(start.plusSeconds(1), context(targetId)).orElseThrow();
        coordinator.tick(start.plusSeconds(2), context(targetId));

        assertEquals(BossAbilityState.COOLDOWN, completed.state());
        assertEquals(List.of("pulse"), effects.executions);
        assertFalse(coordinator.isCooldownReady("pulse", start.plusSeconds(10)));
        assertTrue(coordinator.isCooldownReady("pulse", start.plusSeconds(11)));
    }

    @Test
    void skipsAHigherPriorityAbilityWithoutAValidTarget() {
        UUID bossId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        BossAbilityDefinition unavailable = new BossAbilityDefinition(
            "current-target-only",
            Duration.ZERO,
            Duration.ofSeconds(1),
            BossTargetSelectorDefinition.of(BossTargetSelectorType.CURRENT_TARGET),
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
        BossAbilityCoordinator coordinator = coordinator(
            bossId,
            List.of(unavailable, ability("fallback", 10, Duration.ofSeconds(1), Duration.ZERO, true)),
            new RecordingEffects(),
            new RecordingTelegraphs()
        );

        BossAbilityRuntime runtime = coordinator.tick(
            Instant.parse("2026-07-11T00:00:00Z"),
            context(targetId)
        ).orElseThrow();

        assertEquals("fallback", runtime.abilityId());
    }

    private static BossAbilityCoordinator coordinator(
        UUID bossId,
        List<BossAbilityDefinition> abilities,
        RecordingEffects effects,
        RecordingTelegraphs telegraphs
    ) {
        return new BossAbilityCoordinator(bossId, abilities, effects, telegraphs, new Random(4));
    }

    private static BossAbilityDefinition ability(
        String id,
        int priority,
        Duration castTime,
        Duration cooldown,
        boolean interruptible
    ) {
        return new BossAbilityDefinition(
            id,
            cooldown,
            castTime,
            BossTargetSelectorDefinition.of(BossTargetSelectorType.NEAREST_PLAYER),
            List.of(BossAbilityCondition.noValue(BossAbilityConditionType.NOT_CASTING)),
            List.of(BossAbilityEffectDefinition.withNumber(BossAbilityEffectType.DAMAGE, "amount", 8.0)),
            new BossTelegraphDefinition(BossTelegraphType.CIRCLE, 5, "", ""),
            false,
            priority,
            "cast",
            BossBehaviourCategory.PHYSICAL,
            0.0,
            ""
        );
    }

    private static BossAbilityContext context(UUID targetId) {
        return new BossAbilityContext(
            new BossPosition(0.0, 0.0, 0.0),
            75.0,
            100.0,
            1,
            List.of(new BossTargetCandidate(
                targetId,
                new BossPosition(3.0, 0.0, 0.0),
                20.0,
                20.0,
                100.0,
                true
            )),
            null
        );
    }

    private static final class RecordingEffects implements BossAbilityEffectGateway {
        private final List<String> executions = new ArrayList<>();

        @Override
        public void execute(UUID bossInstanceId, BossAbilityDefinition ability, BossAbilityRuntime runtime) {
            executions.add(ability.id());
        }
    }

    private static final class RecordingTelegraphs implements BossTelegraphGateway {
    }
}
