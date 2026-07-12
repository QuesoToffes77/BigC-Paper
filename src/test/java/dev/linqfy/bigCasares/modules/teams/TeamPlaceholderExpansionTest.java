package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamPlaceholderExpansionTest {

    @Test
    void noTeamProducesSafeEmptyValues() {
        TeamService service = new TeamService(new InMemoryTeamStorage());
        BigCasaresTeamPlaceholderExpansion expansion = new BigCasaresTeamPlaceholderExpansion(service);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000040");

        assertEquals("false", expansion.resolve(playerId, "%bigcasares_team_has_team%"));
        assertEquals("0", expansion.resolve(playerId, "team_member_count"));
        assertEquals("", expansion.resolve(playerId, "team_name"));
        assertEquals("", expansion.resolve(playerId, "team_role"));
    }

    @Test
    void resolvesStoredTeamForDisconnectedPlayerWithoutBukkitOrPlaceholderApi() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000041");
        UUID offlineMember = UUID.fromString("00000000-0000-0000-0000-000000000042");
        TeamService service = new TeamService(new InMemoryTeamStorage());
        service.createTeam(new Team(
            new TeamId(UUID.fromString("50000000-0000-0000-0000-000000000001")),
            "Guardianes del Sur",
            "GDS",
            TeamColor.DARK_AQUA,
            owner,
            Map.of(offlineMember, TeamRole.ADMIN),
            Instant.parse("2026-07-11T12:00:00Z")
        ));
        BigCasaresTeamPlaceholderExpansion expansion = new BigCasaresTeamPlaceholderExpansion(
            service,
            playerId -> playerId.equals(offlineMember) ? "Casares" : "Owner"
        );

        assertEquals("bigcasares", expansion.getIdentifier());
        assertEquals("true", expansion.resolve(offlineMember, "team_has_team"));
        assertEquals("Guardianes del Sur", expansion.resolve(offlineMember, "team_name"));
        assertEquals("GDS", expansion.resolve(offlineMember, "team_tag"));
        assertEquals("DARK_AQUA", expansion.resolve(offlineMember, "team_color"));
        assertEquals("ADMIN", expansion.resolve(offlineMember, "team_role"));
        assertEquals("2", expansion.resolve(offlineMember, "team_member_count"));
        assertEquals("Owner", expansion.resolve(offlineMember, "team_owner"));
        assertEquals("§3§l[GDS]§r §3", expansion.resolve(offlineMember, "team_prefix"));
        assertEquals("§3§l[GDS]§r §3Casares", expansion.resolve(offlineMember, "team_formatted_name"));
    }
}
