package dev.linqfy.bigCasares.modules.auctionhouse;

import java.util.List;

/** Evidence for an item handoff whose player-data save is not atomically coupled to the auction store. */
public record AuctionClaim<I>(List<I> items, String inventoryBefore, String inventoryAfter, List<I> leftovers,
                              String receiptId) {
    public AuctionClaim {
        items = List.copyOf(items);
        leftovers = List.copyOf(leftovers);
        java.util.Objects.requireNonNull(inventoryBefore, "inventoryBefore");
        java.util.UUID.fromString(receiptId);
    }
}
