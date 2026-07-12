package dev.linqfy.bigCasares.modules.teams;

import org.bukkit.scoreboard.Scoreboard;

final class ManagedScoreboardTeams {
    static final String PREFIX = "bc_";

    private ManagedScoreboardTeams() {
    }

    static void removeEntry(Scoreboard scoreboard, String entry, String retainedTeamName) {
        for (org.bukkit.scoreboard.Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(PREFIX) && !team.getName().equals(retainedTeamName)) {
                team.removeEntry(entry);
            }
        }
    }
}
