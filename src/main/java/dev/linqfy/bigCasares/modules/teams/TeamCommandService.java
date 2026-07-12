package dev.linqfy.bigCasares.modules.teams;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public final class TeamCommandService {

    private final TeamService teams;
    private final TeamPresentationService presentation;
    private final TeamInvitationService invitations;
    private final Clock clock;

    public TeamCommandService(
        TeamService teams,
        TeamPresentationService presentation,
        TeamInvitationService invitations,
        Clock clock
    ) {
        this.teams = Objects.requireNonNull(teams, "teams");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.invitations = Objects.requireNonNull(invitations, "invitations");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized Team create(UUID ownerId, String name, String tag, TeamColor color) {
        Objects.requireNonNull(ownerId, "ownerId");
        if (teams.findByMember(ownerId).isPresent()) {
            throw new IllegalArgumentException("Ya perteneces a un equipo.");
        }
        return presentation.createAndRefresh(name, tag, color, ownerId, clock.instant());
    }

    public synchronized Team invite(UUID actorId, UUID invitedPlayerId) {
        Team team = requireTeam(actorId);
        requireOwner(team, actorId);
        if (teams.findByMember(invitedPlayerId).isPresent()) {
            throw new IllegalArgumentException("El jugador ya pertenece a un equipo.");
        }
        invitations.invite(team.id(), invitedPlayerId);
        return team;
    }

    public synchronized Team join(UUID playerId, String tag) {
        if (teams.findByMember(playerId).isPresent()) {
            throw new IllegalArgumentException("Ya perteneces a un equipo.");
        }
        Team team = teams.findByTag(tag)
            .orElseThrow(() -> new IllegalArgumentException("No existe un equipo con ese tag."));
        if (!invitations.hasValidInvitation(team.id(), playerId)) {
            throw new IllegalArgumentException("No tienes una invitación vigente para ese equipo.");
        }
        Team updated = presentation.addMemberAndRefresh(
            team.id(), team.ownerId(), playerId, TeamRole.MEMBER);
        invitations.clearPlayer(playerId);
        return updated;
    }

    public synchronized Team leave(UUID playerId) {
        requireTeam(playerId);
        return presentation.leaveAndRefresh(playerId);
    }

    public synchronized Team kick(UUID actorId, UUID playerId) {
        Team team = requireTeam(actorId);
        requireOwner(team, actorId);
        return presentation.removeMemberAndRefresh(team.id(), actorId, playerId);
    }

    public synchronized Team dissolve(UUID actorId) {
        Team team = requireTeam(actorId);
        requireOwner(team, actorId);
        Team dissolved = presentation.dissolveAndClear(team.id(), actorId);
        invitations.clearTeam(team.id());
        return dissolved;
    }

    private Team requireTeam(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return teams.findByMember(playerId)
            .orElseThrow(() -> new IllegalArgumentException("No perteneces a un equipo."));
    }

    private static void requireOwner(Team team, UUID actorId) {
        if (!team.ownerId().equals(actorId)) {
            throw new SecurityException("Solo el OWNER puede administrar miembros del equipo.");
        }
    }
}
