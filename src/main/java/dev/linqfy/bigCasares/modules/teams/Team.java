package dev.linqfy.bigCasares.modules.teams;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record Team(
    TeamId id,
    String name,
    String tag,
    TeamColor color,
    UUID ownerId,
    Map<UUID, TeamRole> members,
    Instant createdAt,
    TeamTagStyle tagStyle
) {

    public Team(TeamId id, String name, String tag, TeamColor color, UUID ownerId, Map<UUID, TeamRole> members, Instant createdAt) {
        this(id, name, tag, color, ownerId, members, createdAt, TeamTagStyle.DEFAULT);
    }

    public Team {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(members, "members");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(tagStyle, "tagStyle");

        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("El nombre del equipo no puede estar vacío.");
        }
        tag = TeamTagValidator.validate(tag);

        LinkedHashMap<UUID, TeamRole> copiedMembers = new LinkedHashMap<>();
        for (Map.Entry<UUID, TeamRole> entry : members.entrySet()) {
            UUID memberId = Objects.requireNonNull(entry.getKey(), "memberId");
            TeamRole role = Objects.requireNonNull(entry.getValue(), "role");
            if (role == TeamRole.OWNER && !memberId.equals(ownerId)) {
                throw new IllegalArgumentException("Solo el propietario puede tener el rol OWNER.");
            }
            copiedMembers.put(memberId, role);
        }
        copiedMembers.put(ownerId, TeamRole.OWNER);
        members = Collections.unmodifiableMap(copiedMembers);
    }

    public Optional<TeamRole> roleOf(UUID playerId) {
        return Optional.ofNullable(members.get(playerId));
    }

    public boolean containsMember(UUID playerId) {
        return members.containsKey(playerId);
    }

    public Team withTag(String updatedTag) {
        return new Team(id, name, updatedTag, color, ownerId, members, createdAt, tagStyle);
    }

    public Team withColor(TeamColor updatedColor) {
        return new Team(id, name, tag, updatedColor, ownerId, members, createdAt, tagStyle);
    }

    public Team withName(String updatedName) {
        return new Team(id, updatedName, tag, color, ownerId, members, createdAt, tagStyle);
    }

    public Team withTagStyle(TeamTagStyle updatedStyle) {
        return new Team(id, name, tag, color, ownerId, members, createdAt, updatedStyle);
    }

    public Team withMember(UUID playerId, TeamRole role) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(role, "role");
        if (role == TeamRole.OWNER && !playerId.equals(ownerId)) {
            throw new IllegalArgumentException("Solo el propietario puede tener el rol OWNER.");
        }

        LinkedHashMap<UUID, TeamRole> updated = new LinkedHashMap<>(members);
        updated.put(playerId, role);
        return new Team(id, name, tag, color, ownerId, updated, createdAt, tagStyle);
    }

    public Team withoutMember(UUID playerId) {
        if (ownerId.equals(playerId)) {
            throw new IllegalArgumentException("No se puede quitar al propietario del equipo.");
        }

        LinkedHashMap<UUID, TeamRole> updated = new LinkedHashMap<>(members);
        updated.remove(playerId);
        return new Team(id, name, tag, color, ownerId, updated, createdAt, tagStyle);
    }
}
