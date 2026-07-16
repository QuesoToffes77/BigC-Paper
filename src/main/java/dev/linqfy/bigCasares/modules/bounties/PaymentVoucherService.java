package dev.linqfy.bigCasares.modules.bounties;

import java.util.Objects;
import java.util.UUID;

public final class PaymentVoucherService {

    private final PaymentVoucherStorage storage;
    private final BountyEconomy economy;

    public PaymentVoucherService(PaymentVoucherStorage storage, BountyEconomy economy) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.economy = Objects.requireNonNull(economy, "economy");
    }

    public PaymentVoucher issue(double amount) {
        PaymentVoucher voucher = PaymentVoucher.issued(UUID.randomUUID(), amount);
        storage.save(voucher);
        return voucher;
    }

    public synchronized PaymentClaimResult claim(UUID voucherId, UUID claimantId) {
        PaymentVoucher voucher = storage.load(voucherId).orElse(null);
        if (voucher == null) {
            return PaymentClaimResult.NOT_FOUND;
        }
        if (voucher.status() == PaymentVoucherStatus.CLAIMED) {
            return PaymentClaimResult.ALREADY_CLAIMED;
        }
        if (voucher.status() == PaymentVoucherStatus.CLAIMING) {
            return PaymentClaimResult.CLAIM_IN_PROGRESS;
        }

        storage.save(new PaymentVoucher(voucher.id(), voucher.amount(), PaymentVoucherStatus.CLAIMING, claimantId));
        try {
            economy.deposit(claimantId, voucher.amount());
        } catch (RuntimeException ex) {
            // Keep the voucher locked. A Vault provider can fail after applying a payout;
            // retrying automatically would risk paying a copied paper twice.
            return PaymentClaimResult.PAYOUT_FAILED;
        }
        storage.save(new PaymentVoucher(voucher.id(), voucher.amount(), PaymentVoucherStatus.CLAIMED, claimantId));
        return PaymentClaimResult.CLAIMED;
    }

    public double amount(UUID voucherId) {
        return storage.load(voucherId)
                .map(PaymentVoucher::amount)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment voucher: " + voucherId));
    }
}
