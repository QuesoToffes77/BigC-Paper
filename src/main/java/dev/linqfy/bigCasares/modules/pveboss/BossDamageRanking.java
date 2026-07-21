package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class BossDamageRanking {
    private static final Comparator<BossDamageContribution> STANDING_ORDER =
        Comparator.comparingDouble(BossDamageContribution::damage)
            .reversed()
            .thenComparing(BossDamageContribution::reachedAt)
            .thenComparing(contribution -> contribution.playerId().toString());

    public List<BossDamageStanding> topThree(Collection<BossDamageContribution> contributions) {
        Objects.requireNonNull(contributions, "contributions");
        Map<java.util.UUID, BossDamageContribution> bestByPlayer = new LinkedHashMap<>();
        contributions.stream()
            .map(contribution -> Objects.requireNonNull(contribution, "contribution"))
            .filter(contribution -> Double.isFinite(contribution.damage()) && contribution.damage() > 0.0)
            .forEach(contribution -> bestByPlayer.merge(
                contribution.playerId(), contribution, BossDamageRanking::betterSnapshot));
        List<BossDamageContribution> leaders = bestByPlayer.values().stream()
            .sorted(STANDING_ORDER)
            .limit(3)
            .toList();

        return java.util.stream.IntStream.range(0, leaders.size())
            .mapToObj(index -> {
                BossDamageContribution contribution = leaders.get(index);
                return new BossDamageStanding(contribution.playerId(), contribution.damage(), index + 1);
            })
            .toList();
    }

    private static BossDamageContribution betterSnapshot(
        BossDamageContribution first,
        BossDamageContribution second
    ) {
        int damage = Double.compare(first.damage(), second.damage());
        if (damage != 0) {
            return damage > 0 ? first : second;
        }
        return first.reachedAt().compareTo(second.reachedAt()) <= 0 ? first : second;
    }
}
