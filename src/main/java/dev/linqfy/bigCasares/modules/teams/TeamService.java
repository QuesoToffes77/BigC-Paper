package dev.linqfy.bigCasares.modules.teams;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TeamService {

    public static final UUID ADMIN_ACTOR = UUID.fromString("8a9569c1-1925-4e92-b161-a60e42c5232b");

    private final TeamStorage storage;

    public TeamService(TeamStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public synchronized Team createTeam(
        String name,
        String tag,
        TeamColor color,
        UUID ownerId,
        Instant createdAt
    ) {
        return createTeam(TeamId.random(), name, tag, color, ownerId, createdAt);
    }

    public synchronized Team createTeam(
        TeamId id,
        String name,
        String tag,
        TeamColor color,
        UUID ownerId,
        Instant createdAt
    ) {
        String normalizedTag = TeamTagValidator.normalizeAndValidate(tag);
        return createTeam(new Team(id, name, normalizedTag, color, ownerId, Map.of(), createdAt));
    }

    public synchronized Team createTeam(Team team) {
        Objects.requireNonNull(team, "team");
        if (storage.findById(team.id()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un equipo con id " + team.id() + ".");
        }
        ensureTagAvailable(team.tag(), null);
        ensureNameAvailable(team.name(), null);
        for (UUID memberId : team.members().keySet()) {
            storage.findByMember(memberId).ifPresent(existing -> {
                throw new IllegalArgumentException("El jugador " + memberId + " ya pertenece a otro equipo.");
            });
        }
        storage.save(team);
        return team;
    }

    public Optional<Team> findById(TeamId teamId) {
        return storage.findById(teamId);
    }

    public Optional<Team> findByTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        return storage.findByTag(tag.trim());
    }

    public Optional<Team> findByMember(UUID playerId) {
        return storage.findByMember(playerId);
    }

    public Collection<Team> findAll() {
        return storage.findAll();
    }

    public Team requireById(TeamId teamId) {
        return findById(teamId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el equipo " + teamId + "."));
    }

    public synchronized Team updateTag(TeamId teamId, UUID actorId, String newTag) {
        Team current = requireById(teamId);
        requireAppearanceEditor(current, actorId);
        String normalizedTag = TeamTagValidator.normalizeAndValidate(newTag);
        ensureTagAvailable(normalizedTag, teamId);
        Team updated = current.withTag(normalizedTag);
        storage.save(updated);
        return updated;
    }

    public synchronized Team updateColor(TeamId teamId, UUID actorId, TeamColor newColor) {
        Team current = requireById(teamId);
        requireAppearanceEditor(current, actorId);
        Team updated = current.withColor(Objects.requireNonNull(newColor, "newColor"));
        storage.save(updated);
        return updated;
    }

    public synchronized Team rename(TeamId teamId, UUID actorId, String newName) {
        Team current = requireById(teamId);
        requireAppearanceEditor(current, actorId);
        String normalized = Objects.requireNonNull(newName, "newName").trim();
        if (normalized.isEmpty() || normalized.length() > 32) throw new IllegalArgumentException("El nombre debe tener entre 1 y 32 caracteres.");
        ensureNameAvailable(normalized, teamId);
        Team updated = current.withName(normalized);
        storage.save(updated);
        return updated;
    }

    public synchronized Team updateTagStyle(TeamId teamId, UUID actorId, TeamTagStyle style) {
        Team current = requireById(teamId);
        requireAppearanceEditor(current, actorId);
        Team updated = current.withTagStyle(Objects.requireNonNull(style, "style"));
        storage.save(updated);
        return updated;
    }

    public boolean canEditAppearance(TeamId teamId, UUID actorId) {
        return findById(teamId)
            .flatMap(team -> team.roleOf(actorId))
            .map(TeamRole::canEditAppearance)
            .orElse(false);
    }

    public boolean canViewAppearance(TeamId teamId, UUID actorId) {
        return findById(teamId)
            .flatMap(team -> team.roleOf(actorId))
            .map(TeamRole::canViewAppearance)
            .orElse(false);
    }

    public synchronized Team addMember(TeamId teamId, UUID actorId, UUID playerId, TeamRole role) {
        Team current = requireById(teamId);
        requireOwner(current, actorId);
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(role, "role");
        if (role == TeamRole.OWNER) {
            throw new IllegalArgumentException("No se puede añadir otro OWNER.");
        }
        storage.findByMember(playerId).ifPresent(existing -> {
            throw new IllegalArgumentException("El jugador " + playerId + " ya pertenece a otro equipo.");
        });
        Team updated = current.withMember(playerId, role);
        storage.save(updated);
        return updated;
    }

    public synchronized Team updateMemberRole(TeamId teamId, UUID actorId, UUID playerId, TeamRole role) {
        Team current = requireById(teamId);
        requireOwner(current, actorId);
        if (!current.containsMember(playerId)) {
            throw new IllegalArgumentException("El jugador no pertenece al equipo.");
        }
        if (current.ownerId().equals(playerId) || role == TeamRole.OWNER) {
            throw new IllegalArgumentException("El rol OWNER no puede reasignarse.");
        }
        Team updated = current.withMember(playerId, Objects.requireNonNull(role, "role"));
        storage.save(updated);
        return updated;
    }

    public synchronized Team removeMember(TeamId teamId, UUID actorId, UUID playerId) {
        Team current = requireById(teamId);
        requireOwner(current, actorId);
        if (!current.containsMember(playerId)) {
            throw new IllegalArgumentException("El jugador no pertenece al equipo.");
        }
        Team updated = current.withoutMember(playerId);
        storage.save(updated);
        return updated;
    }

    public synchronized Team leaveTeam(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Team current = findByMember(playerId)
            .orElseThrow(() -> new IllegalArgumentException("No perteneces a un equipo."));
        if (current.ownerId().equals(playerId)) {
            throw new IllegalArgumentException("El OWNER debe disolver el equipo en lugar de abandonarlo.");
        }
        Team updated = current.withoutMember(playerId);
        storage.save(updated);
        return updated;
    }

    public synchronized void dissolve(TeamId teamId, UUID actorId) {
        Team current = requireById(teamId);
        requireOwner(current, actorId);
        storage.delete(teamId);
    }

    private void ensureTagAvailable(String tag, TeamId excludedTeamId) {
        storage.findByTag(tag)
            .filter(existing -> excludedTeamId == null || !existing.id().equals(excludedTeamId))
            .ifPresent(existing -> {
                throw new IllegalArgumentException("El tag " + tag + " ya está en uso.");
            });
    }

    private void ensureNameAvailable(String name, TeamId excludedTeamId) {
        storage.findAll().stream()
                .filter(team -> team.name().equalsIgnoreCase(name))
                .filter(team -> excludedTeamId == null || !team.id().equals(excludedTeamId))
                .findFirst().ifPresent(team -> { throw new IllegalArgumentException("El nombre " + name + " ya está en uso."); });
    }

    private void requireAppearanceEditor(Team team, UUID actorId) {
        if (ADMIN_ACTOR.equals(actorId)) return;
        TeamRole role = team.roleOf(actorId).orElse(null);
        if (role == null || !role.canEditAppearance()) {
            throw new SecurityException("Solo el OWNER puede cambiar el tag o el color.");
        }
    }

    private void requireOwner(Team team, UUID actorId) {
        if (ADMIN_ACTOR.equals(actorId)) return;
        if (!team.ownerId().equals(actorId)) {
            throw new SecurityException("Solo el OWNER puede modificar los miembros.");
        }
    }
}
