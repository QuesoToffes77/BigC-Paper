package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossAbilityCastTest {

    @Test
    void updatesTheTelegraphWhileCastingAndCompletesItWithTheEffect() {
        UUID bossId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        RecordingTelegraphs telegraphs = new RecordingTelegraphs();
        List<BossAbilityState> effectStates = new ArrayList<>();
        BossAbilityCoordinator coordinator = new BossAbilityCoordinator(
            bossId,
            List.of(ability(Duration.ofSeconds(3))),
            (ignoredBoss, ignoredAbility, runtime) -> effectStates.add(runtime.state()),
            telegraphs,
            new Random(1)
        );
        Instant start = Instant.parse("2026-07-11T00:00:00Z");

        coordinator.tick(start, context(targetId));
        coordinator.tick(start.plusSeconds(1), context(targetId));
        coordinator.tick(start.plusSeconds(3), context(targetId));

        assertEquals(List.of("begin", "update:PT2S", "complete"), telegraphs.events);
        assertEquals(List.of(BossAbilityState.EXECUTING), effectStates);
    }

    @Test
    void executesAnInstantAbilityInTheTickThatStartsIt() {
        List<BossAbilityState> states = new ArrayList<>();
        BossAbilityCoordinator coordinator = new BossAbilityCoordinator(
            UUID.randomUUID(),
            List.of(ability(Duration.ZERO)),
            (ignoredBoss, ignoredAbility, runtime) -> states.add(runtime.state()),
            new RecordingTelegraphs(),
            new Random(1)
        );

        BossAbilityRuntime runtime = coordinator.tick(
            Instant.parse("2026-07-11T00:00:00Z"),
            context(UUID.randomUUID())
        ).orElseThrow();

        assertEquals(BossAbilityState.COOLDOWN, runtime.state());
        assertEquals(List.of(BossAbilityState.EXECUTING), states);
    }

    private static BossAbilityDefinition ability(Duration castTime) {
        return new BossAbilityDefinition(
            "void-pulse",
            Duration.ofSeconds(5),
            castTime,
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
    }

    private static BossAbilityContext context(UUID targetId) {
        return new BossAbilityContext(
            new BossPosition(0.0, 0.0, 0.0),
            50.0,
            100.0,
            1,
            List.of(new BossTargetCandidate(targetId, new BossPosition(2.0, 0.0, 0.0), 10.0, 20.0, 1.0, true)),
            null
        );
    }

    private static final class RecordingTelegraphs implements BossTelegraphGateway {
        private final List<String> events = new ArrayList<>();

        @Override
        public void begin(UUID bossInstanceId, BossAbilityDefinition ability, BossAbilityRuntime runtime) {
            events.add("begin");
        }

        @Override
        public void update(
            UUID bossInstanceId,
            BossAbilityDefinition ability,
            BossAbilityRuntime runtime,
            Duration remaining
        ) {
            events.add("update:" + remaining);
        }

        @Override
        public void complete(UUID bossInstanceId, BossAbilityDefinition ability, BossAbilityRuntime runtime) {
            events.add("complete");
        }
    }
}
