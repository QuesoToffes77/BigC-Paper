package dev.linqfy.bigCasares.modules.auctionhouse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionHouseServiceTest {

    private static final UUID SELLER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUYER = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-11T12:00:00Z"));
    private final FakeBank bank = new FakeBank();
    private final MemoryStore store = new MemoryStore();
    private AuctionHouseService<String> service;

    @BeforeEach
    void setUp() {
        service = new AuctionHouseService<>(
            new AuctionHouseSettings(2, 10, 1_000, 10, Duration.ofHours(1)), bank, store, clock);
    }

    @Test
    void failedListingSaveDoesNotPublishAnItemStillHeldBySeller() {
        store.fail = true;
        assertThrows(IllegalStateException.class, () -> service.list(SELLER, "Seller", "diamond", 200));
        assertTrue(service.active().isEmpty());
        assertTrue(store.saved.listings().isEmpty());
    }

    @Test
    void interruptedListingDepositRemainsHiddenAfterRestart() {
        AuctionHouseService.ListingPreparation prepared = service.prepareListing(SELLER, "Seller", "diamond", 200, "before");
        assertEquals(AuctionHouseService.ListResult.LISTED, prepared.status());
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertTrue(restarted.active().isEmpty());
        assertEquals("diamond", store.saved.listings().getFirst().item());
        assertEquals("before", store.saved.pendingListings().get(prepared.id()).inventoryBefore());
        assertEquals(AuctionHouseService.BuyStatus.NOT_FOUND, restarted.buy(BUYER, prepared.id()).status());
        assertTrue(restarted.cancel(SELLER, prepared.id()).isEmpty());
        assertTrue(restarted.remove(prepared.id()).isEmpty());
        restarted.addClaims(BUYER, List.of("unrelated"));
        restarted.recordListingOutcome(prepared.id(), "after");
        AuctionHouseService<String> afterRestart = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals("after", afterRestart.pendingListing(SELLER).orElseThrow().getValue().inventoryAfter());
        afterRestart.finishListing(prepared.id());
        assertEquals(1, afterRestart.active().size());
    }

    @Test
    void uncertainWriteFreezesFurtherChangesAndRetainsSavedDepositHold() {
        store.failAfterSave = true;
        assertThrows(IllegalStateException.class, () -> service.prepareListing(SELLER, "Seller", "diamond", 200, "before"));
        store.failAfterSave = false;
        assertThrows(IllegalStateException.class, () -> service.prepareListing(SELLER, "Seller", "diamond", 200, "before"));
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertTrue(restarted.active().isEmpty());
        assertTrue(restarted.pendingListing(SELLER).isPresent());
    }

    @Test
    void validatesPriceAndListingLimit() {
        assertEquals(AuctionHouseService.ListResult.PRICE_TOO_LOW, service.list(SELLER, "Seller", "dirt", 9));
        assertEquals(AuctionHouseService.ListResult.PRICE_TOO_HIGH, service.list(SELLER, "Seller", "dirt", 1_001));
        assertEquals(AuctionHouseService.ListResult.LISTED, service.list(SELLER, "Seller", "dirt", 10));
        assertEquals(AuctionHouseService.ListResult.LISTED, service.list(SELLER, "Seller", "stone", 1_000));
        assertEquals(AuctionHouseService.ListResult.TOO_MANY_LISTINGS, service.list(SELLER, "Seller", "sand", 50));
        assertEquals(2, service.active().size());
    }

    @Test
    void buyingMovesMoneyMinusTaxAndRemovesTheListing() {
        bank.balances.put(BUYER, 500L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();

        AuctionHouseService.BuyResult<String> result = service.buy(BUYER, id);

        assertEquals(AuctionHouseService.BuyStatus.BOUGHT, result.status());
        assertEquals("diamond", result.listing().item());
        assertEquals(300L, bank.balance(BUYER));
        assertEquals(180L, bank.balance(SELLER));
        assertTrue(service.active().isEmpty());
        assertTrue(store.saved.listings().isEmpty());
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals(List.of("diamond"), restarted.claims(BUYER));
        assertEquals(AuctionHouseService.BuyStatus.NOT_FOUND, service.buy(BUYER, id).status());
    }

    @Test
    void refusesOwnListingsAndMissingFunds() {
        bank.balances.put(BUYER, 50L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();

        assertEquals(AuctionHouseService.BuyStatus.OWN_LISTING, service.buy(SELLER, id).status());
        assertEquals(AuctionHouseService.BuyStatus.INSUFFICIENT_FUNDS, service.buy(BUYER, id).status());
        assertEquals(50L, bank.balance(BUYER));
        assertEquals(1, service.active().size());
    }

    @Test
    void refundsTheBuyerWhenTheSellerCannotBePaid() {
        bank.balances.put(BUYER, 500L);
        bank.failDepositsTo = SELLER;
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();

        assertEquals(AuctionHouseService.BuyStatus.PAYMENT_FAILED, service.buy(BUYER, id).status());
        assertEquals(500L, bank.balance(BUYER));
        assertEquals(1, service.active().size());
    }

    @Test
    void uncertainWithdrawalRemainsLockedAfterRestart() {
        bank.balances.put(BUYER, 500L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        bank.throwAfterWithdraw = true;

        assertEquals("PAYMENT_PENDING", service.buy(BUYER, id).status().name());
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals("PAYMENT_PENDING", restarted.buy(BUYER, id).status().name());
        assertTrue(restarted.cancel(SELLER, id).isEmpty());
        assertTrue(restarted.remove(id).isEmpty());
        assertEquals(300L, bank.balance(BUYER));
    }

    @Test
    void failedIntentSaveDoesNotWithdraw() {
        bank.balances.put(BUYER, 500L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        store.failOnSave = 2;
        assertEquals(AuctionHouseService.BuyStatus.PAYMENT_FAILED, service.buy(BUYER, id).status());
        assertEquals(500L, bank.balance(BUYER));
        assertEquals(1, service.active().size());
    }

    @Test
    void failedSaveAfterWithdrawalBlocksPayoutAndSurvivesRestart() {
        bank.balances.put(BUYER, 500L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        store.failOnSave = 3;
        assertEquals("PAYMENT_PENDING", service.buy(BUYER, id).status().name());
        assertEquals(300L, bank.balance(BUYER));
        assertEquals(0L, bank.balance(SELLER));
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        clock.advance(Duration.ofHours(2));
        assertEquals(0, restarted.expire());
        assertEquals("PAYMENT_PENDING", restarted.buy(BUYER, id).status().name());
        assertEquals(300L, bank.balance(BUYER));
    }

    @Test
    void failedFinalSaveKeepsPaidListingLockedAfterRestart() {
        bank.balances.put(BUYER, 500L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        store.failOnSave = 4;
        assertEquals("PAYMENT_PENDING", service.buy(BUYER, id).status().name());
        assertEquals(300L, bank.balance(BUYER));
        assertEquals(180L, bank.balance(SELLER));
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals("PAYMENT_PENDING", restarted.buy(BUYER, id).status().name());
        assertEquals(180L, bank.balance(SELLER));
    }

    @Test
    void uncertainSellerDepositIsNotRepeatedAfterRestart() {
        bank.balances.put(BUYER, 500L);
        bank.throwAfterDepositTo = SELLER;
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        assertEquals("PAYMENT_PENDING", service.buy(BUYER, id).status().name());
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals("PAYMENT_PENDING", restarted.buy(BUYER, id).status().name());
        assertEquals(300L, bank.balance(BUYER));
        assertEquals(180L, bank.balance(SELLER));
    }

    @Test
    void failedRefundRemainsLockedAfterRestart() {
        bank.balances.put(BUYER, 500L);
        bank.failAllDeposits = true;
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        assertEquals("PAYMENT_PENDING", service.buy(BUYER, id).status().name());
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals("PAYMENT_PENDING", restarted.buy(BUYER, id).status().name());
        assertEquals(300L, bank.balance(BUYER));
    }

    @Test
    void expiredListingsGoToTheClaimBox() {
        bank.balances.put(BUYER, 500L);
        service.list(SELLER, "Seller", "diamond", 200);
        long id = service.active().getFirst().id();
        clock.advance(Duration.ofHours(1));

        assertTrue(service.active().isEmpty());
        assertEquals(AuctionHouseService.BuyStatus.NOT_FOUND, service.buy(BUYER, id).status());
        assertEquals(1, service.expire());
        assertEquals(List.of("diamond"), service.claims(SELLER));
        assertEquals(List.of("diamond"), store.saved.claims().get(SELLER));
        assertEquals(List.of("diamond"), service.takeClaims(SELLER));
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals(List.of("diamond"), restarted.claims(SELLER));
        assertTrue(restarted.takeClaims(SELLER).isEmpty());
        assertEquals(500L, bank.balance(BUYER));
    }

    @Test
    void acknowledgedClaimPreservesLeftoversAndNewlyArrivedItems() {
        service.addClaims(BUYER, List.of("diamond", "emerald"));
        assertEquals(List.of("diamond", "emerald"), service.takeClaims(BUYER, "before"));
        service.addClaims(BUYER, List.of("gold"));
        service.recordClaimOutcome(BUYER, "after", List.of("emerald"));
        service.finishClaim(BUYER);
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertEquals(List.of("emerald", "gold"), restarted.claims(BUYER));
        assertTrue(!restarted.hasPendingClaim(BUYER));
    }

    @Test
    void uncertainClaimPersistsInventoryEvidenceAndCannotBeRetried() {
        service.addClaims(BUYER, List.of("diamond"));
        service.takeClaims(BUYER, "before");
        service.recordClaimOutcome(BUYER, "after", List.of());
        AuctionHouseService<String> restarted = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        assertTrue(restarted.hasPendingClaim(BUYER));
        assertEquals("before", store.saved.pendingClaims().get(BUYER).inventoryBefore());
        assertEquals("after", store.saved.pendingClaims().get(BUYER).inventoryAfter());
        assertTrue(restarted.takeClaims(BUYER).isEmpty());
    }

    @Test
    void claimResolutionRequiresTheExactReceiptAndPreservesItemsWhenNotDelivered() {
        service.addClaims(BUYER, List.of("diamond"));
        service.takeClaims(BUYER, "before");
        String id = service.pendingClaim(BUYER).orElseThrow().receiptId();
        assertTrue(!service.resolveClaim(BUYER, UUID.randomUUID().toString(), false));
        assertTrue(service.resolveClaim(BUYER, id, false));
        assertEquals(List.of("diamond"), service.claims(BUYER));
        assertTrue(!service.hasPendingClaim(BUYER));
    }

    @Test
    void listingResolutionRequiresTheExactReceiptAndRetiresAnUnremovedItem() {
        var prepared = service.prepareListing(SELLER, "Seller", "diamond", 200, "before");
        String id = service.pendingListing(SELLER).orElseThrow().getValue().receiptId();
        assertTrue(!service.resolveListing(prepared.id(), UUID.randomUUID().toString(), false));
        assertTrue(service.resolveListing(prepared.id(), id, false));
        assertTrue(service.pendingListing(SELLER).isEmpty());
        assertTrue(store.saved.listings().isEmpty());
    }

    @Test
    void verifiedManualResolutionsCompleteOnlyTheSpecifiedReceipt() {
        service.addClaims(BUYER, List.of("diamond", "emerald"));
        service.takeClaims(BUYER, "before");
        String claimId = service.pendingClaim(BUYER).orElseThrow().receiptId();
        service.recordClaimOutcome(BUYER, "after", List.of("emerald"));
        assertTrue(service.resolveClaim(BUYER, claimId, true));
        assertEquals(List.of("emerald"), service.claims(BUYER));
        var prepared = service.prepareListing(SELLER, "Seller", "gold", 200, "before");
        String listingId = service.pendingListing(SELLER).orElseThrow().getValue().receiptId();
        assertTrue(service.resolveListing(prepared.id(), listingId, true));
        assertEquals(1, service.active().size());
    }

    @Test
    void onlyTheSellerCanCancelAndAdminRemovalKeepsTheItem() {
        service.list(SELLER, "Seller", "diamond", 200);
        service.list(SELLER, "Seller", "emerald", 300);
        long emerald = service.active().get(0).id();
        long diamond = service.active().get(1).id();

        assertEquals(Optional.empty(), service.cancel(BUYER, diamond));
        assertEquals(Optional.of("diamond"), service.cancel(SELLER, diamond));
        assertEquals(List.of("diamond"), new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock).claims(SELLER));
        assertEquals("emerald", service.remove(emerald).orElseThrow().item());
        assertEquals(List.of("diamond", "emerald"), service.claims(SELLER));
        assertTrue(service.listingsOf(SELLER).isEmpty());
    }

    @Test
    void reloadsStateAndKeepsIdsUnique() {
        service.list(SELLER, "Seller", "diamond", 200);
        service.addClaims(BUYER, List.of("leftover"));

        AuctionHouseService<String> reloaded = new AuctionHouseService<>(
            AuctionHouseSettings.defaults(), bank, store, clock);
        reloaded.list(BUYER, "Buyer", "stone", 20);

        assertEquals(2, reloaded.active().size());
        assertEquals(2, reloaded.active().stream().map(AuctionListing::id).distinct().count());
        assertEquals(List.of("leftover"), reloaded.claims(BUYER));
    }

    private static final class FakeBank implements AuctionBank {
        private final Map<UUID, Long> balances = new HashMap<>();
        private UUID failDepositsTo;
        private boolean throwAfterWithdraw;
        private boolean failAllDeposits;
        private UUID throwAfterDepositTo;

        @Override
        public long balance(UUID player) {
            return balances.getOrDefault(player, 0L);
        }

        @Override
        public boolean withdraw(UUID player, long amount) {
            if (balance(player) < amount) {
                return false;
            }
            balances.put(player, balance(player) - amount);
            if (throwAfterWithdraw) throw new IllegalStateException("unknown withdrawal outcome");
            return true;
        }

        @Override
        public boolean deposit(UUID player, long amount) {
            if (failAllDeposits || player.equals(failDepositsTo)) {
                return false;
            }
            balances.put(player, balance(player) + amount);
            if (player.equals(throwAfterDepositTo)) throw new IllegalStateException("unknown deposit outcome");
            return true;
        }
    }

    private static final class MemoryStore implements AuctionStore<String> {
        private AuctionSnapshot<String> saved = AuctionSnapshot.empty();
        private boolean fail;
        private boolean failAfterSave;
        private int saveCount;
        private int failOnSave = -1;

        @Override
        public AuctionSnapshot<String> load() {
            return saved;
        }

        @Override
        public void save(AuctionSnapshot<String> snapshot) {
            if (fail || ++saveCount == failOnSave) throw new IllegalStateException("disk unavailable");
            saved = snapshot;
            if (failAfterSave) throw new IllegalStateException("unknown write result");
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
