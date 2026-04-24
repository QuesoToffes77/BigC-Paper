package dev.linqfy.bigCasares.modules.skillrating;

import java.util.Arrays;

public record SkillRatingSettings(
    double defaultMu,
    double defaultSigma,
    double ordinalSigmaMultiplier,
    double skillRatingScale,
    double[] tierThresholds
) {
    public SkillRatingSettings {
        if (defaultSigma <= 0.0) {
            throw new IllegalArgumentException("default-sigma must be positive");
        }
        if (skillRatingScale <= 0.0) {
            throw new IllegalArgumentException("skill-rating-scale must be positive");
        }
        if (tierThresholds.length != 4) {
            throw new IllegalArgumentException("tier-thresholds must define exactly four values");
        }

        tierThresholds = Arrays.copyOf(tierThresholds, tierThresholds.length);
        for (int i = 1; i < tierThresholds.length; i++) {
            if (tierThresholds[i] <= tierThresholds[i - 1]) {
                throw new IllegalArgumentException("tier-thresholds must be strictly ascending");
            }
        }
    }

    public double[] tierThresholds() {
        return Arrays.copyOf(tierThresholds, tierThresholds.length);
    }

    public double skillRating(double mu, double sigma) {
        return (mu - ordinalSigmaMultiplier * sigma) * skillRatingScale;
    }

    public int tierFor(double skillRating) {
        int tier = 1;
        for (double threshold : tierThresholds) {
            if (skillRating < threshold) {
                return tier;
            }
            tier++;
        }
        return 5;
    }
}
