package dev.linqfy.bigCasares.modules.pveboss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class BossTargetSelectionService {

    private final RandomGenerator random;

    public BossTargetSelectionService(RandomGenerator random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    public Set<UUID> select(BossTargetSelectorDefinition definition, BossAbilityContext context) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(context, "context");

        List<BossTargetCandidate> candidates = eligibleCandidates(definition, context);
        if (candidates.isEmpty()) {
            return Set.of();
        }

        List<BossTargetCandidate> selected = switch (definition.type()) {
            case NEAREST_PLAYER -> firstBy(candidates, Comparator.comparingDouble(
                candidate -> distanceSquared(candidate, context)
            ));
            case FARTHEST_PLAYER -> firstBy(candidates, Comparator.comparingDouble(
                (BossTargetCandidate candidate) -> distanceSquared(candidate, context)
            ).reversed());
            case RANDOM_PLAYER, RANDOM_POSITION -> List.of(candidates.get(random.nextInt(candidates.size())));
            case HIGHEST_DAMAGE -> firstBy(candidates, Comparator.comparingDouble(
                BossTargetCandidate::damageDealt
            ).reversed());
            case LOWEST_HEALTH -> firstBy(candidates, Comparator.comparingDouble(
                BossTargetCandidate::healthRatio
            ));
            case ALL_IN_RADIUS -> candidates;
            case CURRENT_TARGET -> candidates.stream()
                .filter(candidate -> context.currentTarget().map(candidate.targetId()::equals).orElse(false))
                .findFirst()
                .map(List::of)
                .orElseGet(List::of);
        };

        LinkedHashSet<UUID> targetIds = new LinkedHashSet<>();
        selected.stream()
            .limit(definition.maxTargets())
            .map(BossTargetCandidate::targetId)
            .forEach(targetIds::add);
        return Collections.unmodifiableSet(targetIds);
    }

    private static List<BossTargetCandidate> eligibleCandidates(
        BossTargetSelectorDefinition definition,
        BossAbilityContext context
    ) {
        double radiusSquared = definition.radius() * definition.radius();
        List<BossTargetCandidate> result = new ArrayList<>();
        for (BossTargetCandidate candidate : context.targets()) {
            if (candidate.eligible() && distanceSquared(candidate, context) <= radiusSquared) {
                result.add(candidate);
            }
        }
        return result;
    }

    private static List<BossTargetCandidate> firstBy(
        List<BossTargetCandidate> candidates,
        Comparator<BossTargetCandidate> comparator
    ) {
        return candidates.stream().min(comparator).map(List::of).orElseGet(List::of);
    }

    private static double distanceSquared(BossTargetCandidate candidate, BossAbilityContext context) {
        return candidate.position().distanceSquared(context.bossPosition());
    }
}
