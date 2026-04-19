package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MissionRotationPolicyTest {

    @Test
    void assignsConfiguredAmountPerScope() {
        MissionCatalog catalog = MissionCatalog.of(
            List.of(
                definition("daily-a", MissionScope.DAILY),
                definition("daily-b", MissionScope.DAILY),
                definition("daily-c", MissionScope.DAILY),
                definition("weekly-a", MissionScope.WEEKLY),
                definition("weekly-b", MissionScope.WEEKLY)
            )
        );

        MissionRotationPolicy policy = new MissionRotationPolicy(new Random(7));

        MissionPlayerState state = policy.createFreshState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            catalog,
            2,
            1,
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        assertEquals(2, state.dailyAssignments().size());
        assertEquals(1, state.weeklyAssignments().size());
    }

    @Test
    void usesPlayerSeedSoDifferentPlayersCanGetDifferentSelections() {
        MissionCatalog catalog = MissionCatalog.of(
            List.of(
                definition("daily-a", MissionScope.DAILY),
                definition("daily-b", MissionScope.DAILY),
                definition("daily-c", MissionScope.DAILY)
            )
        );

        MissionRotationPolicy policy = new MissionRotationPolicy(new Random(99));

        MissionPlayerState first = policy.createFreshState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            catalog,
            2,
            0,
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );
        MissionPlayerState second = policy.createFreshState(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            catalog,
            2,
            0,
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        assertNotEquals(first.dailyAssignments().keySet(), second.dailyAssignments().keySet());
    }

    private static MissionDefinition definition(String id, MissionScope scope) {
        return new MissionDefinition(
            id,
            scope,
            "Titulo " + id,
            "Descripcion " + id,
            MissionType.HOLD_EXACT_ITEM_COUNT,
            67,
            100.0,
            1,
            true,
            Map.of("material", "TUFF")
        );
    }
}
