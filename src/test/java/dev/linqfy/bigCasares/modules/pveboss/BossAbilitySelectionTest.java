package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossAbilitySelectionTest {

    @Test
    void selectsTheNearestEligibleTargetInsideTheConfiguredRadius() {
        UUID inactive = UUID.randomUUID();
        UUID outside = UUID.randomUUID();
        UUID nearest = UUID.randomUUID();
        BossAbilityContext context = context(List.of(
            target(inactive, 1.0, false),
            target(outside, 15.0, true),
            target(nearest, 4.0, true)
        ));

        Set<UUID> selected = new BossTargetSelectionService(new Random(7)).select(
            new BossTargetSelectorDefinition(BossTargetSelectorType.NEAREST_PLAYER, 10.0, 1),
            context
        );

        assertEquals(Set.of(nearest), selected);
    }

    @Test
    void selectsOnlyEligibleTargetsWhenSelectingEveryoneInRadius() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID inactive = UUID.randomUUID();
        BossAbilityContext context = context(List.of(
            target(first, 2.0, true),
            target(second, 5.0, true),
            target(inactive, 3.0, false)
        ));

        Set<UUID> selected = new BossTargetSelectionService(new Random(7)).select(
            BossTargetSelectorDefinition.allInRadius(6.0),
            context
        );

        assertEquals(Set.of(first, second), selected);
    }

    private static BossAbilityContext context(List<BossTargetCandidate> targets) {
        return new BossAbilityContext(new BossPosition(0.0, 0.0, 0.0), 80.0, 100.0, 1, targets, null);
    }

    private static BossTargetCandidate target(UUID id, double x, boolean eligible) {
        return new BossTargetCandidate(id, new BossPosition(x, 0.0, 0.0), 20.0, 20.0, 0.0, eligible);
    }
}
