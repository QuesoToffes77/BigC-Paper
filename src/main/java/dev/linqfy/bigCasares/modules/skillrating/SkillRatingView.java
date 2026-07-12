package dev.linqfy.bigCasares.modules.skillrating;

import java.util.Locale;

public final class SkillRatingView {

    private SkillRatingView() {
    }

    public static String format(String playerName, SkillRatingState state, double uncertaintyScale) {
        long sk = Math.round(state.skillRating());
        long unc = Math.round((state.sigma() / (25.0 / 3.0)) * 100.0);
        return playerName + " [SK: " + sk + "] (Uncertainty: " + unc + "%)";
    }
}
