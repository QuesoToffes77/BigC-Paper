package dev.linqfy.bigCasares.modules.teams;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TeamYamlMapper {

    public Map<String, Object> toMap(Team team) {
        Objects.requireNonNull(team, "team");

        LinkedHashMap<String, String> members = new LinkedHashMap<>();
        team.members().forEach((memberId, role) -> members.put(memberId.toString(), role.name()));

        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("id", team.id().toString());
        values.put("name", team.name());
        values.put("tag", team.tag());
        values.put("color", team.color().name());
        values.put("owner-id", team.ownerId().toString());
        values.put("members", Collections.unmodifiableMap(members));
        values.put("created-at", team.createdAt().toString());
        LinkedHashMap<String, Object> appearance = new LinkedHashMap<>();
        appearance.put("bold", team.tagStyle().bold());
        appearance.put("italic", team.tagStyle().italic());
        appearance.put("underlined", team.tagStyle().underlined());
        appearance.put("strikethrough", team.tagStyle().strikethrough());
        appearance.put("wrapper", team.tagStyle().wrapper().name());
        values.put("appearance", Collections.unmodifiableMap(appearance));
        return Collections.unmodifiableMap(values);
    }

    public Team fromMap(Map<String, ?> values) {
        Objects.requireNonNull(values, "values");

        TeamId id = TeamId.parse(required(values, "id"));
        String name = required(values, "name");
        String tag = required(values, "tag");
        TeamColor color = TeamColor.parse(required(values, "color"));
        UUID ownerId = UUID.fromString(requiredEither(values, "owner-id", "ownerId"));
        Instant createdAt = Instant.parse(requiredEither(values, "created-at", "createdAt"));

        LinkedHashMap<UUID, TeamRole> members = new LinkedHashMap<>();
        Object rawMembers = values.get("members");
        if (rawMembers != null) {
            if (!(rawMembers instanceof Map<?, ?> memberValues)) {
                throw new IllegalArgumentException("members debe ser un mapa YAML.");
            }
            memberValues.forEach((memberId, role) -> members.put(
                UUID.fromString(String.valueOf(memberId)),
                TeamRole.valueOf(String.valueOf(role))
            ));
        }

        TeamTagStyle style = readStyle(values.get("appearance"));
        return new Team(id, name, tag, color, ownerId, members, createdAt, style);
    }

    private TeamTagStyle readStyle(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return TeamTagStyle.DEFAULT;
        return new TeamTagStyle(
                bool(map, "bold", true), bool(map, "italic", false),
                bool(map, "underlined", false), bool(map, "strikethrough", false),
                map.get("wrapper") == null ? TeamTagWrapper.SQUARE : TeamTagWrapper.parse(String.valueOf(map.get("wrapper")))
        );
    }

    private boolean bool(Map<?, ?> map, String key, boolean fallback) {
        Object value = map.get(key);
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    private String required(Map<String, ?> values, String key) {
        Object value = values.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("Falta el campo YAML obligatorio " + key + ".");
        }
        return String.valueOf(value);
    }

    private String requiredEither(Map<String, ?> values, String preferredKey, String legacyKey) {
        if (values.containsKey(preferredKey)) {
            return required(values, preferredKey);
        }
        return required(values, legacyKey);
    }
}
