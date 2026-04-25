package dev.linqfy.bigCasares.modules.skillrating;

import java.util.Locale;

public final class SkillRatingView {

    private SkillRatingView() {
    }

    public static String format(String playerName, SkillRatingState state) {
        return String.format(
            Locale.US,
            "Skill Rating de %s: %.0f | Tier %d | mu %.2f | sigma %.2f",
            playerName,
            state.skillRating(),
            state.tier(),
            state.mu(),
            state.sigma()
        );
    }
}
