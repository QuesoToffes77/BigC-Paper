package dev.linqfy.bigCasares.modules.auctionhouse;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Fixed-price auction house. Sellers list an item, buyers pay the price through the {@link AuctionBank}
 * and the seller receives the price minus the configured tax. Expired or cancelled items that cannot be
 * handed back directly wait in the seller's claim box. Every change is saved through the {@link AuctionStore}.
 * Not thread-safe: call it from the server thread.
 */
public final class AuctionHouseService<I> {

    public enum ListResult { LISTED, PRICE_TOO_LOW, PRICE_TOO_HIGH, TOO_MANY_LISTINGS }

    public record ListingPreparation(ListResult status, long id) { }

    public enum BuyStatus { BOUGHT, NOT_FOUND, OWN_LISTING, INSUFFICIENT_FUNDS, PAYMENT_FAILED, PAYMENT_PENDING }

    public record BuyResult<I>(BuyStatus status, AuctionListing<I> listing) {
    }

    private static final Comparator<AuctionListing<?>> NEWEST_FIRST =
        Comparator.comparing((AuctionListing<?> listing) -> listing.createdAt()).reversed()
            .thenComparing(Comparator.comparingLong((AuctionListing<?> listing) -> listing.id()).reversed());

    private final AuctionHouseSettings settings;
    private final AuctionBank bank;
    private final AuctionStore<I> store;
    private final Clock clock;
    private final Map<Long, AuctionListing<I>> listings = new LinkedHashMap<>();
    private final Map<UUID, List<I>> claims = new LinkedHashMap<>();
    private final Map<Long, AuctionPurchase> pendingPurchases = new LinkedHashMap<>();
    private final Map<UUID, AuctionClaim<I>> pendingClaims = new LinkedHashMap<>();
    private final Map<Long, AuctionListingHold> pendingListings = new LinkedHashMap<>();
    private long nextId;
    private boolean storageFaulted;

    public AuctionHouseService(AuctionHouseSettings settings, AuctionBank bank, AuctionStore<I> store, Clock clock) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.bank = Objects.requireNonNull(bank, "bank");
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        AuctionSnapshot<I> snapshot = store.load();
        snapshot.listings().forEach(listing -> listings.put(listing.id(), listing));
        snapshot.claims().forEach((player, items) -> claims.put(player, new ArrayList<>(items)));
        pendingPurchases.putAll(snapshot.pendingPurchases());
        pendingClaims.putAll(snapshot.pendingClaims());
        pendingListings.putAll(snapshot.pendingListings());
        nextId = snapshot.nextId();
    }

    public AuctionHouseSettings settings() {
        return settings;
    }

    public ListingPreparation prepareListing(UUID seller, String sellerName, I item, long price, String before) {
        Objects.requireNonNull(item, "item");
        if (price < settings.minPrice()) return new ListingPreparation(ListResult.PRICE_TOO_LOW, 0);
        if (price > settings.maxPrice()) return new ListingPreparation(ListResult.PRICE_TOO_HIGH, 0);
        if (listingsOf(seller).size() >= settings.maxListingsPerPlayer()
            || pendingListings.values().stream().anyMatch(hold -> hold.seller().equals(seller))) {
            return new ListingPreparation(ListResult.TOO_MANY_LISTINGS, 0);
        }
        Instant now = clock.instant();
        AuctionListing<I> listing = new AuctionListing<>(nextId, seller, sellerName, item, price,
            now, now.plus(settings.listingDuration()));
        AuctionListingHold hold = new AuctionListingHold(seller, before, null, UUID.randomUUID().toString());
        List<AuctionListing<I>> proposed = new ArrayList<>(listings.values());
        proposed.add(listing);
        Map<Long, AuctionListingHold> pending = new LinkedHashMap<>(pendingListings);
        pending.put(listing.id(), hold);
        saveSafely(new AuctionSnapshot<>(nextId + 1, proposed, claims, pendingPurchases, pendingClaims, pending));
        nextId++;
        listings.put(listing.id(), listing);
        pendingListings.put(listing.id(), hold);
        return new ListingPreparation(ListResult.LISTED, listing.id());
    }

    public Optional<Map.Entry<Long, AuctionListingHold>> pendingListing(UUID seller) {
        return pendingListings.entrySet().stream().filter(entry -> entry.getValue().seller().equals(seller))
            .map(entry -> Map.entry(entry.getKey(), entry.getValue())).findFirst();
    }

    public void recordListingOutcome(long id, String after) {
        AuctionListingHold hold = Objects.requireNonNull(pendingListings.get(id), "pending listing");
        AuctionListingHold updated = new AuctionListingHold(hold.seller(), hold.inventoryBefore(),
            Objects.requireNonNull(after), hold.receiptId());
        Map<Long, AuctionListingHold> pending = new LinkedHashMap<>(pendingListings);
        pending.put(id, updated);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, pendingPurchases, pendingClaims, pending));
        pendingListings.put(id, updated);
    }

    public void finishListing(long id) {
        AuctionListingHold hold = Objects.requireNonNull(pendingListings.get(id), "pending listing");
        if (hold.inventoryAfter() == null) throw new IllegalStateException("Listing outcome is unknown");
        Map<Long, AuctionListingHold> pending = new LinkedHashMap<>(pendingListings);
        pending.remove(id);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, pendingPurchases, pendingClaims, pending));
        pendingListings.remove(id);
    }

    /** Inserts an item that has already been escrowed by the caller. */
    ListResult list(UUID seller, String sellerName, I item, long price) {
        Objects.requireNonNull(item, "item");
        if (price < settings.minPrice()) {
            return ListResult.PRICE_TOO_LOW;
        }
        if (price > settings.maxPrice()) {
            return ListResult.PRICE_TOO_HIGH;
        }
        if (listingsOf(seller).size() >= settings.maxListingsPerPlayer()) {
            return ListResult.TOO_MANY_LISTINGS;
        }
        Instant now = clock.instant();
        AuctionListing<I> listing = new AuctionListing<>(
            nextId, seller, sellerName, item, price, now, now.plus(settings.listingDuration()));
        List<AuctionListing<I>> proposed = new ArrayList<>(listings.values());
        proposed.add(listing);
        saveSafely(new AuctionSnapshot<>(nextId + 1, proposed, claims, pendingPurchases, pendingClaims, pendingListings));
        nextId++;
        listings.put(listing.id(), listing);
        return ListResult.LISTED;
    }

    /** Charges the buyer, pays the seller, and places the purchased item in the buyer's claim box. */
    public BuyResult<I> buy(UUID buyer, long listingId) {
        if (storageFaulted) return new BuyResult<>(BuyStatus.PAYMENT_PENDING, listings.get(listingId));
        AuctionListing<I> listing = listings.get(listingId);
        if (pendingListings.containsKey(listingId)) return new BuyResult<>(BuyStatus.NOT_FOUND, listing);
        if (pendingPurchases.containsKey(listingId)) {
            return new BuyResult<>(BuyStatus.PAYMENT_PENDING, listing);
        }
        if (listing == null || listing.expired(clock.instant())) {
            return new BuyResult<>(BuyStatus.NOT_FOUND, listing);
        }
        if (listing.seller().equals(buyer)) {
            return new BuyResult<>(BuyStatus.OWN_LISTING, listing);
        }
        if (bank.balance(buyer) < listing.price()) {
            return new BuyResult<>(BuyStatus.INSUFFICIENT_FUNDS, listing);
        }
        pendingPurchases.put(listingId, new AuctionPurchase(buyer, AuctionPurchase.Phase.WITHDRAW));
        try {
            saveChecked();
        } catch (RuntimeException failure) {
            pendingPurchases.remove(listingId);
            return new BuyResult<>(BuyStatus.PAYMENT_FAILED, listing);
        }
        try {
            if (!bank.withdraw(buyer, listing.price())) {
                return finishFailedPurchase(listingId, listing);
            }
            pendingPurchases.put(listingId, new AuctionPurchase(buyer, AuctionPurchase.Phase.PAYOUT));
            saveChecked();
            long payout = listing.price() - settings.taxOf(listing.price());
            if (!bank.deposit(listing.seller(), payout)) {
                pendingPurchases.put(listingId, new AuctionPurchase(buyer, AuctionPurchase.Phase.REFUND));
                saveChecked();
                if (!bank.deposit(buyer, listing.price())) {
                    return new BuyResult<>(BuyStatus.PAYMENT_PENDING, listing);
                }
                return finishFailedPurchase(listingId, listing);
            }
            List<AuctionListing<I>> remaining = new ArrayList<>(listings.values());
            remaining.remove(listing);
            Map<Long, AuctionPurchase> remainingPurchases = new LinkedHashMap<>(pendingPurchases);
            remainingPurchases.remove(listingId);
            Map<UUID, List<I>> updatedClaims = new LinkedHashMap<>(claims);
            List<I> buyerClaims = new ArrayList<>(claims.getOrDefault(buyer, List.of()));
            buyerClaims.add(listing.item());
            updatedClaims.put(buyer, buyerClaims);
            saveSafely(new AuctionSnapshot<>(nextId, remaining, updatedClaims, remainingPurchases, pendingClaims, pendingListings));
            listings.remove(listingId);
            pendingPurchases.remove(listingId);
            claims.put(buyer, buyerClaims);
            return new BuyResult<>(BuyStatus.BOUGHT, listing);
        } catch (RuntimeException failure) {
            // Vault or disk may have completed the operation before throwing. Never retry automatically.
            return new BuyResult<>(BuyStatus.PAYMENT_PENDING, listing);
        }
    }

    private BuyResult<I> finishFailedPurchase(long listingId, AuctionListing<I> listing) {
        Map<Long, AuctionPurchase> remaining = new LinkedHashMap<>(pendingPurchases);
        remaining.remove(listingId);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, remaining, pendingClaims, pendingListings));
        pendingPurchases.remove(listingId);
        return new BuyResult<>(BuyStatus.PAYMENT_FAILED, listing);
    }

    /** Moves the seller's own listing into their claim box. */
    public Optional<I> cancel(UUID seller, long listingId) {
        AuctionListing<I> listing = listings.get(listingId);
        if (listing == null || pendingPurchases.containsKey(listingId) || pendingListings.containsKey(listingId)
            || !listing.seller().equals(seller)) {
            return Optional.empty();
        }
        moveToClaim(listing);
        return Optional.of(listing.item());
    }

    /** Admin removal: the item goes back to the seller's claim box. */
    public Optional<AuctionListing<I>> remove(long listingId) {
        if (pendingPurchases.containsKey(listingId) || pendingListings.containsKey(listingId)) return Optional.empty();
        AuctionListing<I> listing = listings.get(listingId);
        if (listing == null) {
            return Optional.empty();
        }
        moveToClaim(listing);
        return Optional.of(listing);
    }

    private void moveToClaim(AuctionListing<I> listing) {
        List<AuctionListing<I>> remaining = new ArrayList<>(listings.values());
        remaining.remove(listing);
        Map<UUID, List<I>> updated = new LinkedHashMap<>(claims);
        List<I> sellerClaims = new ArrayList<>(claims.getOrDefault(listing.seller(), List.of()));
        sellerClaims.add(listing.item());
        updated.put(listing.seller(), sellerClaims);
        saveSafely(new AuctionSnapshot<>(nextId, remaining, updated, pendingPurchases, pendingClaims, pendingListings));
        listings.remove(listing.id());
        claims.put(listing.seller(), sellerClaims);
    }

    /** Moves expired listings into their sellers' claim boxes; returns how many expired. */
    public int expire() {
        Instant now = clock.instant();
        List<AuctionListing<I>> expired = listings.values().stream().filter(listing -> !pendingPurchases.containsKey(listing.id())
            && !pendingListings.containsKey(listing.id()) && listing.expired(now)).toList();
        if (!expired.isEmpty()) {
            List<AuctionListing<I>> remaining = new ArrayList<>(listings.values());
            Map<UUID, List<I>> updated = new LinkedHashMap<>(claims);
            for (AuctionListing<I> listing : expired) {
                remaining.remove(listing);
                List<I> items = new ArrayList<>(updated.getOrDefault(listing.seller(), List.of()));
                items.add(listing.item());
                updated.put(listing.seller(), items);
            }
            saveSafely(new AuctionSnapshot<>(nextId, remaining, updated, pendingPurchases, pendingClaims, pendingListings));
            expired.forEach(listing -> listings.remove(listing.id()));
            claims.clear();
            claims.putAll(updated);
        }
        return expired.size();
    }

    public Optional<AuctionListing<I>> find(long listingId) {
        AuctionListing<I> listing = listings.get(listingId);
        return listing == null || pendingPurchases.containsKey(listingId) || pendingListings.containsKey(listingId)
            || listing.expired(clock.instant())
            ? Optional.empty() : Optional.of(listing);
    }

    /** Listings that can still be bought, newest first. */
    public List<AuctionListing<I>> active() {
        Instant now = clock.instant();
        return listings.values().stream().filter(listing -> !pendingPurchases.containsKey(listing.id())
            && !pendingListings.containsKey(listing.id()) && !listing.expired(now)).sorted(NEWEST_FIRST).toList();
    }

    /** Every listing of the seller that has not been moved to the claim box yet, newest first. */
    public List<AuctionListing<I>> listingsOf(UUID seller) {
        return listings.values().stream().filter(listing -> listing.seller().equals(seller)).sorted(NEWEST_FIRST).toList();
    }

    public List<I> claims(UUID player) {
        return List.copyOf(claims.getOrDefault(player, List.of()));
    }

    /** Records an attempted handoff before returning the items. */
    public List<I> takeClaims(UUID player) {
        return takeClaims(player, "");
    }

    public List<I> takeClaims(UUID player, String inventoryBefore) {
        List<I> taken = claims.get(player);
        if (taken == null || taken.isEmpty() || pendingClaims.containsKey(player)) {
            return List.of();
        }
        AuctionClaim<I> receipt = new AuctionClaim<>(taken, inventoryBefore, null, List.of(), UUID.randomUUID().toString());
        Map<UUID, AuctionClaim<I>> pending = new LinkedHashMap<>(pendingClaims);
        pending.put(player, receipt);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, pendingPurchases, pending, pendingListings));
        pendingClaims.put(player, receipt);
        return List.copyOf(taken);
    }

    public boolean hasPendingClaim(UUID player) { return pendingClaims.containsKey(player); }
    public Optional<AuctionClaim<I>> pendingClaim(UUID player) { return Optional.ofNullable(pendingClaims.get(player)); }

    /** Applies an administrator's verified decision; never invokes Vault or delivers items. */
    public boolean resolveClaim(UUID player, String receiptId, boolean delivered) {
        AuctionClaim<I> receipt = pendingClaims.get(player);
        if (receipt == null || !receipt.receiptId().equals(receiptId)) return false;
        if (delivered) {
            finishClaim(player);
        } else {
            Map<UUID, AuctionClaim<I>> pending = new LinkedHashMap<>(pendingClaims);
            pending.remove(player);
            saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, pendingPurchases, pending, pendingListings));
            pendingClaims.remove(player);
        }
        return true;
    }

    /** Applies an administrator's verified decision without moving the player's inventory. */
    public boolean resolveListing(long id, String receiptId, boolean deposited) {
        AuctionListingHold hold = pendingListings.get(id);
        if (hold == null || !hold.receiptId().equals(receiptId)) return false;
        Map<Long, AuctionListingHold> pending = new LinkedHashMap<>(pendingListings);
        pending.remove(id);
        List<AuctionListing<I>> remaining = new ArrayList<>(listings.values());
        if (!deposited) remaining.remove(listings.get(id));
        saveSafely(new AuctionSnapshot<>(nextId, remaining, claims, pendingPurchases, pendingClaims, pending));
        pendingListings.remove(id);
        if (!deposited) listings.remove(id);
        return true;
    }

    public void recordClaimOutcome(UUID player, String inventoryAfter, List<I> leftovers) {
        AuctionClaim<I> receipt = Objects.requireNonNull(pendingClaims.get(player), "pending claim");
        AuctionClaim<I> updated = new AuctionClaim<>(receipt.items(), receipt.inventoryBefore(),
            Objects.requireNonNull(inventoryAfter), leftovers, receipt.receiptId());
        Map<UUID, AuctionClaim<I>> pending = new LinkedHashMap<>(pendingClaims);
        pending.put(player, updated);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, pendingPurchases, pending, pendingListings));
        pendingClaims.put(player, updated);
    }

    public void finishClaim(UUID player) {
        AuctionClaim<I> receipt = Objects.requireNonNull(pendingClaims.get(player), "pending claim");
        if (receipt.inventoryAfter() == null) throw new IllegalStateException("Claim outcome is unknown");
        List<I> current = claims.getOrDefault(player, List.of());
        int count = receipt.items().size();
        if (current.size() < count || !current.subList(0, count).equals(receipt.items())) {
            throw new IllegalStateException("Claim box changed during handoff");
        }
        List<I> remaining = new ArrayList<>(receipt.leftovers());
        remaining.addAll(current.subList(count, current.size()));
        Map<UUID, List<I>> updatedClaims = new LinkedHashMap<>(claims);
        if (remaining.isEmpty()) updatedClaims.remove(player); else updatedClaims.put(player, remaining);
        Map<UUID, AuctionClaim<I>> pending = new LinkedHashMap<>(pendingClaims);
        pending.remove(player);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), updatedClaims, pendingPurchases, pending, pendingListings));
        if (remaining.isEmpty()) claims.remove(player); else claims.put(player, remaining);
        pendingClaims.remove(player);
    }

    /** Puts items (for example leftovers that did not fit in an inventory) into the player's claim box. */
    public void addClaims(UUID player, List<I> items) {
        if (items.isEmpty()) {
            return;
        }
        List<I> combined = new ArrayList<>(claims.getOrDefault(player, List.of()));
        combined.addAll(items);
        Map<UUID, List<I>> updated = new LinkedHashMap<>(claims);
        updated.put(player, combined);
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), updated, pendingPurchases, pendingClaims, pendingListings));
        claims.put(player, combined);
    }

    private void saveChecked() {
        saveSafely(new AuctionSnapshot<>(nextId, List.copyOf(listings.values()), claims, pendingPurchases, pendingClaims, pendingListings));
    }

    private void saveSafely(AuctionSnapshot<I> snapshot) {
        if (storageFaulted) throw new IllegalStateException("Auction storage needs reconciliation after a failed write");
        try {
            store.saveListing(snapshot);
        } catch (RuntimeException failure) {
            storageFaulted = true;
            throw failure;
        }
    }

}
