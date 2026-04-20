package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyServiceTest {

    @Test
    void paysExistingBountyAndRefreshesFromRemainingBalance() {
        InMemoryBountyStorage storage = new InMemoryBountyStorage();
        FakeEconomyGateway economy = new FakeEconomyGateway();
        BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 25.0);
        BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

        UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        storage.save(new BountyPlayerState(victimId, 150.0, Instant.parse("2026-04-19T11:00:00Z")));
        economy.setBalance(victimId, 500.0);

        BountyProcessingResult result = service.handlePlayerKill(victimId, killerId);

        assertEquals(150.0, result.paidOutAmount());
        assertEquals(100.0, result.newBountyAmount());
        assertEquals(150.0, economy.totalDepositedTo(killerId));
        assertEquals(150.0, economy.totalWithdrawnFrom(victimId));
        assertEquals(100.0, storage.load(victimId).orElseThrow().activeBounty());
        assertTrue(result.createdNewBounty());
    }

    @Test
    void stacksNewBountyOnExistingStoredAmountWhenNoPayoutHappensBeforeRefresh() {
        InMemoryBountyStorage storage = new InMemoryBountyStorage();
        FakeEconomyGateway economy = new FakeEconomyGateway();
        BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 25.0);
        BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

        UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        storage.save(new BountyPlayerState(victimId, 40.0, Instant.parse("2026-04-19T11:00:00Z")));

        BountyPlayerState stacked = service.stackGeneratedBounty(victimId, 60.0);

        assertEquals(100.0, stacked.activeBounty());
    }

    @Test
    void skipsRefreshWhenVictimBalanceIsBelowMinimumBalance() {
        InMemoryBountyStorage storage = new InMemoryBountyStorage();
        FakeEconomyGateway economy = new FakeEconomyGateway();
        BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 25.0);
        BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

        UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000005");

        economy.setBalance(victimId, 80.0);

        BountyProcessingResult result = service.handlePlayerKill(victimId, killerId);

        assertEquals(0.0, result.newBountyAmount());
        assertEquals(0.0, economy.totalWithdrawnFrom(victimId));
        assertEquals(0.0, storage.load(victimId).orElseThrow().activeBounty());
    }

    @Test
    void skipsRefreshWhenComputedBountyFallsBelowMinimumBounty() {
        InMemoryBountyStorage storage = new InMemoryBountyStorage();
        FakeEconomyGateway economy = new FakeEconomyGateway();
        BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 40.0);
        BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

        UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000006");
        UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        economy.setBalance(victimId, 150.0);

        BountyProcessingResult result = service.handlePlayerKill(victimId, killerId);

        assertEquals(0.0, result.newBountyAmount());
        assertEquals(0.0, economy.totalWithdrawnFrom(victimId));
        assertEquals(0.0, storage.load(victimId).orElseThrow().activeBounty());
    }

    @Test
    void rejectsSettingsWhenBountyPercentExceedsTakePercent() {
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new BountySettings(0.20, 0.30, 100.0, 25.0)
        );

        assertTrue(thrown.getMessage().contains("bounty-percent"));
    }

    private static final class InMemoryBountyStorage implements BountyStorage {
        private final Map<UUID, BountyPlayerState> states = new HashMap<>();

        @Override
        public java.util.Optional<BountyPlayerState> load(UUID playerId) {
            return java.util.Optional.ofNullable(states.get(playerId));
        }

        @Override
        public void save(BountyPlayerState state) {
            states.put(state.playerId(), state);
        }
    }

    private static final class FakeEconomyGateway implements BountyEconomy {
        private final Map<UUID, Double> balances = new HashMap<>();
        private final Map<UUID, Double> deposits = new HashMap<>();
        private final Map<UUID, Double> withdrawals = new HashMap<>();

        void setBalance(UUID playerId, double amount) {
            balances.put(playerId, amount);
        }

        double totalDepositedTo(UUID playerId) {
            return deposits.getOrDefault(playerId, 0.0);
        }

        double totalWithdrawnFrom(UUID playerId) {
            return withdrawals.getOrDefault(playerId, 0.0);
        }

        @Override
        public double balance(UUID playerId) {
            return balances.getOrDefault(playerId, 0.0);
        }

        @Override
        public void withdraw(UUID playerId, double amount) {
            balances.put(playerId, balance(playerId) - amount);
            withdrawals.merge(playerId, amount, Double::sum);
        }

        @Override
        public void deposit(UUID playerId, double amount) {
            deposits.merge(playerId, amount, Double::sum);
        }

        @Override
        public String format(double amount) {
            return "$" + amount;
        }
    }
}
