package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentVoucherServiceTest {

    @Test
    void paymentPaperCanBeClaimedOnlyOnce() {
        InMemoryStorage storage = new InMemoryStorage();
        FakeEconomy economy = new FakeEconomy();
        PaymentVoucherService service = new PaymentVoucherService(storage, economy);
        UUID firstHolderId = UUID.randomUUID();
        UUID copiedPaperHolderId = UUID.randomUUID();
        PaymentVoucher voucher = service.issue(125.0);

        assertEquals(PaymentClaimResult.CLAIMED, service.claim(voucher.id(), firstHolderId));
        assertEquals(125.0, economy.balance(firstHolderId));
        assertEquals(PaymentClaimResult.ALREADY_CLAIMED, service.claim(voucher.id(), copiedPaperHolderId));
        assertEquals(0.0, economy.balance(copiedPaperHolderId));
    }

    private static final class InMemoryStorage implements PaymentVoucherStorage {
        private final Map<UUID, PaymentVoucher> vouchers = new HashMap<>();

        @Override
        public Optional<PaymentVoucher> load(UUID voucherId) {
            return Optional.ofNullable(vouchers.get(voucherId));
        }

        @Override
        public void save(PaymentVoucher voucher) {
            vouchers.put(voucher.id(), voucher);
        }
    }

    private static final class FakeEconomy implements BountyEconomy {
        private final Map<UUID, Double> balances = new HashMap<>();

        @Override
        public double balance(UUID playerId) {
            return balances.getOrDefault(playerId, 0.0);
        }

        @Override
        public void withdraw(UUID playerId, double amount) {
            balances.put(playerId, balance(playerId) - amount);
        }

        @Override
        public void deposit(UUID playerId, double amount) {
            balances.put(playerId, balance(playerId) + amount);
        }

        @Override
        public String format(double amount) {
            return String.valueOf(amount);
        }
    }
}
