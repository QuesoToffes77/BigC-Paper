package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamPresentationServiceTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000030");
    private static final UUID MEMBER = UUID.fromString("00000000-0000-0000-0000-000000000031");
    private static final UUID OUTSIDER = UUID.fromString("00000000-0000-0000-0000-000000000032");

    private TeamId teamId;
    private TeamService teamService;
    private RecordingGateway gateway;
    private TeamPresentationService presentationService;

    @BeforeEach
    void setUp() {
        teamId = new TeamId(UUID.fromString("40000000-0000-0000-0000-000000000001"));
        teamService = new TeamService(new InMemoryTeamStorage());
        teamService.createTeam(new Team(
            teamId,
            "Guardianes del Sur",
            "GDS",
            TeamColor.DARK_AQUA,
            OWNER,
            Map.of(MEMBER, TeamRole.MEMBER),
            Instant.parse("2026-07-11T12:00:00Z")
        ));
        gateway = new RecordingGateway();
        presentationService = new TeamPresentationService(teamService, gateway);
    }

    @Test
    void prefixResetsBoldBeforeApplyingPlayerColor() {
        TeamPresentation presentation = TeamPresentation.from(teamService.requireById(teamId));

        assertEquals("§3§l[GDS]§r §3", presentation.formattedPrefix());
        assertEquals("§3§l[GDS]§r §3Casares", presentation.formatPlayerName("Casares"));
    }

    @Test
    void refreshPlayerTargetsOnlyThatPlayerAndClearsPlayersWithoutATeam() {
        presentationService.refreshPlayer(MEMBER);
        presentationService.refreshPlayer(OUTSIDER);

        assertEquals(List.of(MEMBER), gateway.refreshedPlayers);
        assertEquals(List.of(OUTSIDER), gateway.clearedPlayers);
        assertEquals("GDS", gateway.lastPresentation.tag());
    }

    @Test
    void colorChangeRefreshesOnlyTheAffectedTeam() {
        presentationService.updateColorAndRefresh(teamId, OWNER, TeamColor.GOLD);

        assertEquals(List.of(teamId), gateway.refreshedTeams);
        assertTrue(gateway.refreshedPlayers.isEmpty());
        assertEquals(TeamColor.GOLD, teamService.requireById(teamId).color());
    }

    private static final class RecordingGateway implements TeamNamePresentationGateway {
        private final List<UUID> refreshedPlayers = new ArrayList<>();
        private final List<UUID> clearedPlayers = new ArrayList<>();
        private final List<TeamId> refreshedTeams = new ArrayList<>();
        private TeamPresentation lastPresentation;

        @Override
        public void refreshPlayer(UUID playerId, TeamPresentation presentation) {
            refreshedPlayers.add(playerId);
            lastPresentation = presentation;
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
