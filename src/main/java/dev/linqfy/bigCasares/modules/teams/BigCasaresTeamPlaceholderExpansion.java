package dev.linqfy.bigCasares.modules.teams;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class BigCasaresTeamPlaceholderExpansion extends PlaceholderExpansion implements TeamPlaceholderValueResolver {

    public static final String IDENTIFIER = "bigcasares";

    private final TeamService teamService;
    private final Function<UUID, String> playerNameResolver;

    public BigCasaresTeamPlaceholderExpansion(TeamService teamService) {
        this(teamService, UUID::toString);
    }

    public BigCasaresTeamPlaceholderExpansion(
        TeamService teamService,
        Function<UUID, String> playerNameResolver
    ) {
        this.teamService = Objects.requireNonNull(teamService, "teamService");
        this.playerNameResolver = Objects.requireNonNull(playerNameResolver, "playerNameResolver");
    }

    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public @NotNull String getAuthor() {
        return "BigCasares";
    }

    @Override
    public @NotNull String getVersion() {
        return "2";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return player == null ? "" : resolve(player.getUniqueId(), params);
    }

    @Override
    public String resolve(UUID playerId, String placeholder) {
        String key = normalizePlaceholder(placeholder);
        Team team = teamService.findByMember(playerId).orElse(null);
        if (team == null) {
            return switch (key) {
                case "has_team" -> "false";
                case "member_count" -> "0";
                default -> "";
            };
        }

        TeamPresentation presentation = TeamPresentation.from(team);
        return switch (key) {
            case "name" -> team.name();
            case "tag" -> team.tag();
            case "color" -> team.color().name();
            case "prefix" -> presentation.formattedPrefix();
            case "formatted_name" -> presentation.formatPlayerName(resolvePlayerName(playerId));
            case "role" -> team.roleOf(playerId).map(Enum::name).orElse("");
            case "owner" -> resolvePlayerName(team.ownerId());
            case "member_count" -> Integer.toString(team.members().size());
            case "has_team" -> "true";
            default -> "";
        };
    }

    public String resolvePlaceholder(UUID playerId, String placeholder) {
        return resolve(playerId, placeholder);
    }

    private String resolvePlayerName(UUID playerId) {
        String resolved = playerNameResolver.apply(playerId);
        return resolved == null || resolved.isBlank() ? playerId.toString() : resolved;
    }

    private String normalizePlaceholder(String placeholder) {
        if (placeholder == null) {
            return "";
        }
        String normalized = placeholder.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("%") && normalized.endsWith("%") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized.startsWith(IDENTIFIER + "_")) {
            normalized = normalized.substring(IDENTIFIER.length() + 1);
        }
        if (normalized.startsWith("team_")) {
            normalized = normalized.substring("team_".length());
        }
        return normalized;
    }
}
