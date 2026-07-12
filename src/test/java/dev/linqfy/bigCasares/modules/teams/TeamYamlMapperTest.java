package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamYamlMapperTest {

    @Test
    void roundTripPreservesCreatedAtAndEveryMemberRole() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000020");
        UUID admin = UUID.fromString("00000000-0000-0000-0000-000000000021");
        UUID member = UUID.fromString("00000000-0000-0000-0000-000000000022");
        Team original = new Team(
            new TeamId(UUID.fromString("30000000-0000-0000-0000-000000000001")),
            "Guardianes del Sur",
            "GDS",
            TeamColor.DARK_AQUA,
            owner,
            Map.of(admin, TeamRole.ADMIN, member, TeamRole.MEMBER),
            Instant.parse("2026-07-11T12:34:56Z")
        );

        TeamYamlMapper mapper = new TeamYamlMapper();
        Team restored = mapper.fromMap(mapper.toMap(original));

        assertEquals(original, restored);
        assertEquals(Instant.parse("2026-07-11T12:34:56Z"), restored.createdAt());
        assertEquals(TeamRole.OWNER, restored.members().get(owner));
        assertEquals(TeamRole.ADMIN, restored.members().get(admin));
        assertEquals(TeamRole.MEMBER, restored.members().get(member));
    }
}
