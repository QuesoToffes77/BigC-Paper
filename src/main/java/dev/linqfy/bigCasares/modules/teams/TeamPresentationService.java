package dev.linqfy.bigCasares.modules.teams;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class TeamPresentationService {

    private final TeamService teamService;
    private final TeamNamePresentationGateway gateway;

    public TeamPresentationService(TeamService teamService, TeamNamePresentationGateway gateway) {
        this.teamService = Objects.requireNonNull(teamService, "teamService");
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public void refreshPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        teamService.findByMember(playerId).ifPresentOrElse(
            team -> gateway.refreshPlayer(playerId, TeamPresentation.from(team)),
            () -> gateway.clearPlayer(playerId)
        );
    }

    public void clearPlayer(UUID playerId) {
        gateway.clearPlayer(Objects.requireNonNull(playerId, "playerId"));
    }

    public void refreshTeam(TeamId teamId) {
        teamService.requireById(teamId);
        gateway.refreshTeam(teamId);
    }

    public Team updateTagAndRefresh(TeamId teamId, UUID actorId, String tag) {
        Team updated = teamService.updateTag(teamId, actorId, tag);
        gateway.refreshTeam(teamId);
        return updated;
    }

    public Team updateColorAndRefresh(TeamId teamId, UUID actorId, TeamColor color) {
        Team updated = teamService.updateColor(teamId, actorId, color);
        gateway.refreshTeam(teamId);
        return updated;
    }

    public Team renameAndRefresh(TeamId teamId, UUID actorId, String name) {
        Team updated = teamService.rename(teamId, actorId, name);
        gateway.refreshTeam(teamId);
        return updated;
    }

    public Team updateStyleAndRefresh(TeamId teamId, UUID actorId, TeamTagStyle style) {
        Team updated = teamService.updateTagStyle(teamId, actorId, style);
        gateway.refreshTeam(teamId);
        return updated;
    }

    public Team createAndRefresh(
        String name,
        String tag,
        TeamColor color,
        UUID ownerId,
        Instant createdAt
    ) {
        Team created = teamService.createTeam(name, tag, color, ownerId, createdAt);
        gateway.refreshPlayer(ownerId, TeamPresentation.from(created));
        return created;
    }

    public Team addMemberAndRefresh(TeamId teamId, UUID actorId, UUID playerId, TeamRole role) {
        Team updated = teamService.addMember(teamId, actorId, playerId, role);
        gateway.refreshTeam(teamId);
        return updated;
    }

    public Team removeMemberAndRefresh(TeamId teamId, UUID actorId, UUID playerId) {
        Team updated = teamService.removeMember(teamId, actorId, playerId);
        gateway.clearPlayer(playerId);
        gateway.refreshTeam(teamId);
        return updated;
    }

    public Team leaveAndRefresh(UUID playerId) {
        Team current = teamService.findByMember(playerId)
            .orElseThrow(() -> new IllegalArgumentException("No perteneces a un equipo."));
        Team updated = teamService.leaveTeam(playerId);
        gateway.clearPlayer(playerId);
        gateway.refreshTeam(current.id());
        return updated;
    }

    public Team dissolveAndClear(TeamId teamId, UUID actorId) {
        Team current = teamService.requireById(teamId);
        List<UUID> members = List.copyOf(current.members().keySet());
        teamService.dissolve(teamId, actorId);
        members.forEach(gateway::clearPlayer);
        return current;
    }
}
