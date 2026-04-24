package dev.linqfy.bigCasares.modules.skillrating;

public record SkillRatingUpdate(
    SkillRatingState killerState,
    SkillRatingState victimState,
    int previousKillerTier
) {
    public boolean killerTierChanged() {
        return killerState.tier() != previousKillerTier;
    }
}
