package dev.linqfy.bigCasares.modules.auctionhouse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistent state of listings, claim boxes, and purchases awaiting reconciliation. */
public record AuctionSnapshot<I>(long nextId, List<AuctionListing<I>> listings, Map<UUID, List<I>> claims,
                                 Map<Long, AuctionPurchase> pendingPurchases, Map<UUID, AuctionClaim<I>> pendingClaims,
                                 Map<Long, AuctionListingHold> pendingListings) {

    public AuctionSnapshot {
        listings = List.copyOf(listings);
        pendingPurchases = Map.copyOf(pendingPurchases);
        pendingClaims = Map.copyOf(pendingClaims);
        pendingListings = Map.copyOf(pendingListings);
        if (!listings.stream().map(AuctionListing::id).collect(java.util.stream.Collectors.toSet())
            .containsAll(pendingPurchases.keySet())) {
            throw new IllegalArgumentException("Pending purchase without listing");
        }
        for (Map.Entry<Long, AuctionListingHold> hold : pendingListings.entrySet()) {
            if (pendingPurchases.containsKey(hold.getKey()) || listings.stream().noneMatch(listing ->
                listing.id() == hold.getKey() && listing.seller().equals(hold.getValue().seller()))) {
                throw new IllegalArgumentException("Pending deposit without matching listing");
            }
        }
        claims = claims.entrySet().stream()
            .filter(entry -> !entry.getValue().isEmpty())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        for (Map.Entry<UUID, AuctionClaim<I>> pending : pendingClaims.entrySet()) {
            AuctionClaim<I> receipt = pending.getValue();
            List<I> box = claims.getOrDefault(pending.getKey(), List.of());
            if (receipt.items().isEmpty() || box.size() < receipt.items().size()
                || !box.subList(0, receipt.items().size()).equals(receipt.items())) {
                throw new IllegalArgumentException("Pending claim does not match claim box");
            }
        }
        long highestId = listings.stream().mapToLong(AuctionListing::id).max().orElse(0);
        nextId = Math.max(nextId, highestId + 1);
    }

    public AuctionSnapshot(long nextId, List<AuctionListing<I>> listings, Map<UUID, List<I>> claims) {
        this(nextId, listings, claims, Map.of(), Map.of(), Map.of());
    }

    public AuctionSnapshot(long nextId, List<AuctionListing<I>> listings, Map<UUID, List<I>> claims,
                           Map<Long, AuctionPurchase> pendingPurchases) {
        this(nextId, listings, claims, pendingPurchases, Map.of(), Map.of());
    }

    public AuctionSnapshot(long nextId, List<AuctionListing<I>> listings, Map<UUID, List<I>> claims,
                           Map<Long, AuctionPurchase> pendingPurchases, Map<UUID, AuctionClaim<I>> pendingClaims) {
        this(nextId, listings, claims, pendingPurchases, pendingClaims, Map.of());
    }

    public static <I> AuctionSnapshot<I> empty() {
        return new AuctionSnapshot<>(1, List.of(), Map.of());
    }
}
