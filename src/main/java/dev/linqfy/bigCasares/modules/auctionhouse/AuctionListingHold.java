package dev.linqfy.bigCasares.modules.auctionhouse;

import java.util.UUID;

/** Records an item deposit until a matching saved player-data checkpoint is verified. */
public record AuctionListingHold(UUID seller, String inventoryBefore, String inventoryAfter, String receiptId) {
    public AuctionListingHold {
        java.util.Objects.requireNonNull(seller, "seller");
        java.util.Objects.requireNonNull(inventoryBefore, "inventoryBefore");
        UUID.fromString(receiptId);
    }
}
