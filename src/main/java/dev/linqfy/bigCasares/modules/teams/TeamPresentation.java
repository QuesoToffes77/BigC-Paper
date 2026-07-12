package dev.linqfy.bigCasares.modules.teams;

import java.util.Objects;

public record TeamPresentation(
    String teamName,
    String tag,
    TeamColor color,
    String formattedPrefix
) {

    private static final String RESET = "§r";

    public TeamPresentation {
        Objects.requireNonNull(teamName, "teamName");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(formattedPrefix, "formattedPrefix");
    }

    public static TeamPresentation from(Team team) {
        Objects.requireNonNull(team, "team");
        String color = team.color().legacyCode();
        String prefix = color + team.tagStyle().legacyDecorations()
                + team.tagStyle().wrapper().wrap(team.tag()) + RESET + " " + color;
        return new TeamPresentation(team.name(), team.tag(), team.color(), prefix);
    }

    public String formatPlayerName(String playerName) {
        return formattedPrefix + Objects.requireNonNull(playerName, "playerName");
    }
}
