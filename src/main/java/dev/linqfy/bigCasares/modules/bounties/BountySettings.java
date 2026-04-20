package dev.linqfy.bigCasares.modules.bounties;

public record BountySettings(
    double takePercent,
    double bountyPercent,
    double minimumBalance,
    double minimumBounty
) {
    public BountySettings {
        if (takePercent <= 0.0) {
            throw new IllegalArgumentException("take-percent debe ser mayor que 0.");
        }
        if (bountyPercent < 0.0) {
            throw new IllegalArgumentException("bounty-percent no puede ser negativo.");
        }
        if (bountyPercent > takePercent) {
            throw new IllegalArgumentException("bounty-percent no puede ser mayor que take-percent.");
        }
        if (minimumBalance < 0.0) {
            throw new IllegalArgumentException("minimum-balance no puede ser negativo.");
        }
        if (minimumBounty < 0.0) {
            throw new IllegalArgumentException("minimum-bounty no puede ser negativo.");
        }
    }

    public double bleedPercent() {
        return takePercent - bountyPercent;
    }
}
