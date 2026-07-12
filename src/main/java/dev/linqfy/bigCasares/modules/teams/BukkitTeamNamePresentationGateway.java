package dev.linqfy.bigCasares.modules.teams;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Objects;
import java.util.UUID;

public final class BukkitTeamNamePresentationGateway implements TeamNamePresentationGateway {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Server server;
    private final TeamService teamService;
    private final boolean scoreboardPrefix;
    private final boolean tabListName;
    private final boolean displayName;

    public BukkitTeamNamePresentationGateway(
        Server server,
        TeamService teamService,
        boolean scoreboardPrefix,
        boolean tabListName,
        boolean displayName
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.teamService = Objects.requireNonNull(teamService, "teamService");
        this.scoreboardPrefix = scoreboardPrefix;
        this.tabListName = tabListName;
        this.displayName = displayName;
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
            String teamKey = scoreboardKey(presentation.tag());
            ManagedScoreboardTeams.removeEntry(scoreboard, player.getName(), teamKey);
            org.bukkit.scoreboard.Team scoreboardTeam = scoreboard.getTeam(teamKey);
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.registerNewTeam(teamKey);
            }
            scoreboardTeam.prefix(prefix);
            scoreboardTeam.addEntry(player.getName());
        }
        Component formattedName = prefix.append(Component.text(player.getName()));
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
        ManagedScoreboardTeams.removeEntry(scoreboard, player.getName(), null);
        if (tabListName) {
            player.playerListName(Component.text(player.getName()));
        }
        if (displayName) {
            player.displayName(Component.text(player.getName()));
        }
    }

    @Override
    public void refreshTeam(TeamId teamId) {
        Team team = teamService.requireById(teamId);
        TeamPresentation presentation = TeamPresentation.from(team);
        team.members().keySet().forEach(member -> refreshPlayer(member, presentation));
    }

    private static String scoreboardKey(String tag) {
        return ManagedScoreboardTeams.PREFIX + tag.toLowerCase(java.util.Locale.ROOT);
    }
}
