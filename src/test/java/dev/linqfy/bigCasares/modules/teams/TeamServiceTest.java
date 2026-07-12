package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamServiceTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000012");

    @Test
    void teamSnapshotDefensivelyCopiesMembersAndAlwaysIncludesOwner() {
        Map<UUID, TeamRole> suppliedMembers = new LinkedHashMap<>();
        suppliedMembers.put(ADMIN, TeamRole.ADMIN);

        Team team = team(suppliedMembers);
        suppliedMembers.put(MEMBER, TeamRole.MEMBER);

        assertEquals(TeamRole.OWNER, team.members().get(OWNER));
        assertEquals(TeamRole.ADMIN, team.members().get(ADMIN));
        assertFalse(team.members().containsKey(MEMBER));
        assertThrows(UnsupportedOperationException.class, () -> team.members().put(MEMBER, TeamRole.MEMBER));
    }

    @Test
    void onlyOwnerCanChangeTagOrColorWhileAdminCanInspectAppearance() {
        TeamService service = new TeamService(new InMemoryTeamStorage());
        service.createTeam(team(Map.of(ADMIN, TeamRole.ADMIN, MEMBER, TeamRole.MEMBER)));

        Team changedTag = service.updateTag(teamId(), OWNER, "NEW1");
        Team changedColor = service.updateColor(teamId(), OWNER, TeamColor.GOLD);

        assertEquals("NEW1", changedTag.tag());
        assertEquals(TeamColor.GOLD, changedColor.color());
        assertTrue(service.canViewAppearance(teamId(), OWNER));
        assertTrue(service.canViewAppearance(teamId(), ADMIN));
        assertFalse(service.canViewAppearance(teamId(), MEMBER));
        assertThrows(SecurityException.class, () -> service.updateTag(teamId(), ADMIN, "NOPE"));
        assertThrows(SecurityException.class, () -> service.updateColor(teamId(), MEMBER, TeamColor.RED));
    }

    @Test
    void replacingAStoredTeamDoesNotExposeMutableStorageState() {
        InMemoryTeamStorage storage = new InMemoryTeamStorage();
        TeamService service = new TeamService(storage);
        Team original = service.createTeam(team(Map.of(MEMBER, TeamRole.MEMBER)));

        Team updated = service.updateColor(original.id(), OWNER, TeamColor.AQUA);

        assertEquals(TeamColor.DARK_AQUA, original.color());
        assertEquals(TeamColor.AQUA, updated.color());
        assertEquals(TeamColor.AQUA, storage.findById(original.id()).orElseThrow().color());
    }

    @Test
    void memberCanLeaveWhileOwnerMustDissolve() {
        TeamService service = new TeamService(new InMemoryTeamStorage());
        service.createTeam(team(Map.of(MEMBER, TeamRole.MEMBER)));

        Team updated = service.leaveTeam(MEMBER);

        assertFalse(updated.containsMember(MEMBER));
        assertTrue(service.findByMember(MEMBER).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.leaveTeam(OWNER));
    }

    private static Team team(Map<UUID, TeamRole> members) {
        return new Team(
            teamId(),
            "Guardianes del Sur",
            "GDS",
            TeamColor.DARK_AQUA,
            OWNER,
            members,
            Instant.parse("2026-07-11T12:00:00Z")
        );
    }

    private static TeamId teamId() {
        return new TeamId(UUID.fromString("20000000-0000-0000-0000-000000000001"));
    }
}
