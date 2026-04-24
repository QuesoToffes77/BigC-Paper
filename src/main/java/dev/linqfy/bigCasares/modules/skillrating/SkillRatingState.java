package dev.linqfy.bigCasares.modules.skillrating;

import java.time.Instant;
import java.util.UUID;

public record SkillRatingState(
    UUID playerId,
    double mu,
    double sigma,
    double skillRating,
    int tier,
    Instant updatedAt
) {
    public static SkillRatingState initial(UUID playerId, SkillRatingSettings settings, Instant updatedAt) {
        double skillRating = settings.skillRating(settings.defaultMu(), settings.defaultSigma());
        return new SkillRatingState(
            playerId,
            settings.defaultMu(),
            settings.defaultSigma(),
            skillRating,
            settings.tierFor(skillRating),
            updatedAt
        );
    }
}
