package dev.linqfy.bigCasares.modules.bounties;

public record BountyProcessingResult(
    double paidOutAmount,
    double newBountyAmount,
    double totalTakenAmount,
    double bleedAmount,
    boolean createdNewBounty,
    boolean paidExistingBounty
) {
    public static BountyProcessingResult empty() {
        return new BountyProcessingResult(0.0, 0.0, 0.0, 0.0, false, false);
    }
}
