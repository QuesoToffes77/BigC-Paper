package dev.linqfy.bigCasares.modules.teams;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class BukkitTeamNamePresentationGateway implements TeamNamePresentationGateway {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Server server;
    private final TeamService teamService;
    private final boolean scoreboardPrefix;
    private final boolean tabListName;
    private final boolean displayName;
    private final Function<UUID, net.kyori.adventure.text.format.NamedTextColor> nameColor;

    public BukkitTeamNamePresentationGateway(
        Server server,
        TeamService teamService,
        boolean scoreboardPrefix,
        boolean tabListName,
        boolean displayName
    ) {
        this(server, teamService, scoreboardPrefix, tabListName, displayName, ignored -> net.kyori.adventure.text.format.NamedTextColor.GRAY);
    }

    public BukkitTeamNamePresentationGateway(
        Server server,
        TeamService teamService,
        boolean scoreboardPrefix,
        boolean tabListName,
        boolean displayName,
        Function<UUID, net.kyori.adventure.text.format.NamedTextColor> nameColor
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.teamService = Objects.requireNonNull(teamService, "teamService");
        this.scoreboardPrefix = scoreboardPrefix;
        this.tabListName = tabListName;
        this.displayName = displayName;
        this.nameColor = Objects.requireNonNull(nameColor, "nameColor");
    }

    @Override
    public void refreshPlayer(UUID playerId, TeamPresentation presentation) {
        Player player = server.getPlayer(playerId);
        if (player == null) {
            return;
        }
        Component prefix = LEGACY.deserialize(presentation.formattedPrefix());
        if (scoreboardPrefix) {
            Scoreboard scoreboard = server.getScoreboardManager().getMainScoreboard();
            var color = nameColor.apply(playerId);
            String teamKey = scoreboardKey(presentation.tag(), color);
            ManagedScoreboardTeams.removeEntry(scoreboard, player.getName(), teamKey);
            org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.getTeam(teamKey);
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.registerNewTeam(teamKey);
            }
            scoreboardTeam.prefix(prefix);
            scoreboardTeam.color(color);
            scoreboardTeam.addEntry(player.getName());
        }
        Component formattedName = prefix.append(Component.text(player.getName(), nameColor.apply(playerId)));
        if (tabListName) {
            player.playerListName(formattedName);
        }
        if (displayName) {
            player.displayName(formattedName);
        }
    }

    @Override
    public void clearPlayer(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboardManager().getMainScoreboard();
        var color = nameColor.apply(playerId);
        String teamKey = "bc_danger_" + color.toString().substring(0, Math.min(4, color.toString().length()));
        ManagedScoreboardTeams.removeEntry(scoreboard, player.getName(), teamKey);
        org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.getTeam(teamKey);
        if (scoreboardTeam == null) scoreboardTeam = scoreboard.registerNewTeam(teamKey);
        scoreboardTeam.prefix(Component.empty());
        scoreboardTeam.color(color);
        scoreboardTeam.addEntry(player.getName());
        if (tabListName) {
            player.playerListName(Component.text(player.getName(), nameColor.apply(playerId)));
        }
        if (displayName) {
            player.displayName(Component.text(player.getName(), nameColor.apply(playerId)));
        }
    }

    @Override
    public void refreshTeam(TeamId teamId) {
        Team team = teamService.requireById(teamId);
        TeamPresentation presentation = TeamPresentation.from(team);
        team.members().keySet().forEach(member -> refreshPlayer(member, presentation));
    }

    private static String scoreboardKey(String tag, net.kyori.adventure.text.format.NamedTextColor color) {
        String hash = Integer.toUnsignedString(tag.toLowerCase(java.util.Locale.ROOT).hashCode(), 36);
        if (hash.length() > 8) hash = hash.substring(0, 8);
        return ManagedScoreboardTeams.PREFIX + "t" + hash + "_" + color.toString().charAt(0);
    }
}
