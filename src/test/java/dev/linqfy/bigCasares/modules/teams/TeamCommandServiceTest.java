package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamCommandServiceTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000061");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000062");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-000000000063");

    private TeamService teams;
    private TeamInvitationService invitations;
    private RecordingGateway gateway;
    private TeamCommandService commands;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T12:00:00Z"), ZoneOffset.UTC);
        teams = new TeamService(new InMemoryTeamStorage());
        invitations = new TeamInvitationService(clock, Duration.ofMinutes(5));
        gateway = new RecordingGateway();
        TeamPresentationService presentation = new TeamPresentationService(teams, gateway);
        commands = new TeamCommandService(teams, presentation, invitations, clock);
    }

    @Test
    void createBuildsAUsableTeamAndRefreshesItsOwner() {
        Team created = commands.create(OWNER, "Guardianes", "gds", TeamColor.DARK_AQUA);

        assertEquals(created, teams.findByMember(OWNER).orElseThrow());
        assertEquals("GDS", created.tag());
        assertEquals(List.of(OWNER), gateway.refreshedPlayers);
    }

    @Test
    void invitedPlayerCanJoinByTagAndInvitationIsConsumed() {
        Team created = commands.create(OWNER, "Guardianes", "GDS", TeamColor.DARK_AQUA);
        commands.invite(OWNER, MEMBER);

        Team joined = commands.join(MEMBER, "gds");

        assertEquals(TeamRole.MEMBER, joined.roleOf(MEMBER).orElseThrow());
        assertEquals(List.of(created.id()), gateway.refreshedTeams);
        assertFalse(invitations.hasValidInvitation(created.id(), MEMBER));
        assertThrows(IllegalArgumentException.class, () -> commands.join(MEMBER, "GDS"));
    }

    @Test
    void onlyOwnerCanInviteOrKickMembers() {
        Team team = commands.create(OWNER, "Guardianes", "GDS", TeamColor.DARK_AQUA);
        teams.addMember(team.id(), OWNER, MEMBER, TeamRole.MEMBER);

        assertThrows(SecurityException.class, () -> commands.invite(MEMBER, OUTSIDER));
        assertThrows(SecurityException.class, () -> commands.kick(MEMBER, OWNER));

        Team updated = commands.kick(OWNER, MEMBER);

        assertFalse(updated.containsMember(MEMBER));
        assertEquals(List.of(MEMBER), gateway.clearedPlayers);
        assertTrue(gateway.refreshedTeams.contains(team.id()));
    }

    @Test
    void memberCanLeaveButOwnerMustDissolve() {
        Team team = commands.create(OWNER, "Guardianes", "GDS", TeamColor.DARK_AQUA);
        teams.addMember(team.id(), OWNER, MEMBER, TeamRole.MEMBER);

        Team updated = commands.leave(MEMBER);

        assertFalse(updated.containsMember(MEMBER));
        assertTrue(teams.findByMember(MEMBER).isEmpty());
        assertTrue(gateway.clearedPlayers.contains(MEMBER));
        assertThrows(IllegalArgumentException.class, () -> commands.leave(OWNER));
    }

    @Test
    void dissolveClearsAllPresentationsAndOutstandingInvitations() {
        Team team = commands.create(OWNER, "Guardianes", "GDS", TeamColor.DARK_AQUA);
        teams.addMember(team.id(), OWNER, MEMBER, TeamRole.MEMBER);
        commands.invite(OWNER, OUTSIDER);

        Team dissolved = commands.dissolve(OWNER);

        assertEquals(team.id(), dissolved.id());
        assertTrue(teams.findById(team.id()).isEmpty());
        assertTrue(gateway.clearedPlayers.containsAll(List.of(OWNER, MEMBER)));
        assertFalse(invitations.hasValidInvitation(team.id(), OUTSIDER));
    }

    private static final class RecordingGateway implements TeamNamePresentationGateway {
        private final List<UUID> refreshedPlayers = new ArrayList<>();
        private final List<UUID> clearedPlayers = new ArrayList<>();
        private final List<TeamId> refreshedTeams = new ArrayList<>();

        @Override
        public void refreshPlayer(UUID playerId, TeamPresentation presentation) {
            refreshedPlayers.add(playerId);
        }

        @Override
        public void clearPlayer(UUID playerId) {
            clearedPlayers.add(playerId);
        }

        @Override
        public void refreshTeam(TeamId teamId) {
            refreshedTeams.add(teamId);
        }
    }
}
