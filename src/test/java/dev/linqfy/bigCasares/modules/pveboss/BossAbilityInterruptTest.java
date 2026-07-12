package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossAbilityInterruptTest {

    @Test
    void interruptsOnlyAnInterruptibleCast() {
        RecordingTelegraphs telegraphs = new RecordingTelegraphs();
        BossAbilityCoordinator coordinator = coordinator(true, telegraphs);
        Instant now = Instant.parse("2026-07-11T00:00:00Z");
        coordinator.tick(now, context());

        boolean interrupted = coordinator.interruptCast(now.plusSeconds(1));

        assertTrue(interrupted);
        assertTrue(coordinator.activeCast().isEmpty());
        assertEquals(BossAbilityState.INTERRUPTED, coordinator.lastRuntime().orElseThrow().state());
        assertEquals(List.of("cancel:INTERRUPTED"), telegraphs.events);
    }

    @Test
    void refusesToInterruptANonInterruptibleCast() {
        BossAbilityCoordinator coordinator = coordinator(false, new RecordingTelegraphs());
        Instant now = Instant.parse("2026-07-11T00:00:00Z");
        coordinator.tick(now, context());

        assertFalse(coordinator.interruptCast(now.plusSeconds(1)));
        assertTrue(coordinator.activeCast().isPresent());
    }

    @Test
    void cancellingTheBossStopsEvenANonInterruptibleCastAndDisablesTheCoordinator() {
        RecordingTelegraphs telegraphs = new RecordingTelegraphs();
        BossAbilityCoordinator coordinator = coordinator(false, telegraphs);
        Instant now = Instant.parse("2026-07-11T00:00:00Z");
        coordinator.tick(now, context());

        coordinator.cancelAll(now.plusSeconds(1));

        assertTrue(coordinator.isDisabled());
        assertEquals(BossAbilityState.DISABLED, coordinator.lastRuntime().orElseThrow().state());
        assertTrue(coordinator.tick(now.plusSeconds(2), context()).isEmpty());
        assertEquals(List.of("cancel:DISABLED"), telegraphs.events);
    }

    private static BossAbilityCoordinator coordinator(boolean interruptible, RecordingTelegraphs telegraphs) {
        BossAbilityDefinition ability = new BossAbilityDefinition(
            "long-cast",
            Duration.ofSeconds(5),
            Duration.ofSeconds(10),
            BossTargetSelectorDefinition.of(BossTargetSelectorType.NEAREST_PLAYER),
            List.of(),
            List.of(BossAbilityEffectDefinition.of(BossAbilityEffectType.DAMAGE)),
            new BossTelegraphDefinition(BossTelegraphType.CIRCLE, 5, "", ""),
            interruptible,
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
            telegraphs,
            new Random(1)
        );
    }

    private static BossAbilityContext context() {
        return new BossAbilityContext(
            new BossPosition(0.0, 0.0, 0.0),
            10.0,
            10.0,
            1,
            List.of(new BossTargetCandidate(UUID.randomUUID(), new BossPosition(1.0, 0.0, 0.0), 10.0, 10.0, 0.0, true)),
            null
        );
    }

    private static final class RecordingTelegraphs implements BossTelegraphGateway {
        private final List<String> events = new ArrayList<>();

        @Override
        public void cancel(
            UUID bossInstanceId,
            BossAbilityDefinition ability,
            BossAbilityRuntime runtime
        ) {
            events.add("cancel:" + runtime.state());
        }
    }
}
