package dev.linqfy.bigCasares.modules.pveboss;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BossDamageRankingTest {
    private final BossDamageRanking ranking = new BossDamageRanking();

    @Test
    void standingsSortByDamageThenReachedTimeThenUuidAndStopAtThree() {
        UUID lowestUuid = id(1);
        UUID higherUuid = id(2);
        UUID lowerDamage = id(3);
        UUID fourth = id(4);
        Instant early = Instant.parse("2026-07-18T00:00:01Z");
        Instant late = Instant.parse("2026-07-18T00:00:02Z");

        List<BossDamageStanding> standings = ranking.topThree(List.of(
            new BossDamageContribution(fourth, 25.0, early),
            new BossDamageContribution(lowerDamage, 50.0, early),
            new BossDamageContribution(higherUuid, 100.0, late),
            new BossDamageContribution(lowestUuid, 100.0, late),
            new BossDamageContribution(id(5), 100.0, early)
        ));

        assertEquals(List.of(id(5), lowestUuid, higherUuid),
            standings.stream().map(BossDamageStanding::playerId).toList());
        assertEquals(List.of(1, 2, 3),
            standings.stream().map(BossDamageStanding::placement).toList());
    }

    @Test
    void excludesNonpositiveDamage() {
        List<BossDamageStanding> standings = ranking.topThree(List.of(
            new BossDamageContribution(id(1), 0.0, Instant.EPOCH),
            new BossDamageContribution(id(2), -1.0, Instant.EPOCH),
            new BossDamageContribution(id(3), 1.0, Instant.EPOCH)
        ));

        assertEquals(List.of(id(3)), standings.stream().map(BossDamageStanding::playerId).toList());
    }

    @Test
    void duplicatePlayerSnapshotsCanNeverOccupyMultiplePlacements() {
        Instant early = Instant.parse("2026-07-18T00:00:01Z");
        Instant late = Instant.parse("2026-07-18T00:00:02Z");

        List<BossDamageStanding> standings = ranking.topThree(List.of(
            new BossDamageContribution(id(1), 50.0, early),
            new BossDamageContribution(id(1), 100.0, late),
            new BossDamageContribution(id(2), 75.0, early)
        ));

        assertEquals(List.of(id(1), id(2)),
            standings.stream().map(BossDamageStanding::playerId).toList());
        assertEquals(List.of(100.0, 75.0),
            standings.stream().map(BossDamageStanding::damage).toList());
    }

    private static UUID id(long value) {
        return new UUID(0L, value);
    }
}
