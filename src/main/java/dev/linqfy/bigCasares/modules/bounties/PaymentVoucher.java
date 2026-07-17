package dev.linqfy.bigCasares.modules.bounties;

import java.util.Objects;
import java.util.UUID;

public record PaymentVoucher(
        UUID id,
        double amount,
        PaymentVoucherStatus status,
        UUID claimedBy
) {
    public PaymentVoucher {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(status, "status");
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("Payment voucher amount must be finite and positive");
        }
        if (status == PaymentVoucherStatus.CLAIMED && claimedBy == null) {
            throw new IllegalArgumentException("Claimed payment vouchers require a claimant");
        }
    }

    public static PaymentVoucher issued(UUID id, double amount) {
        return new PaymentVoucher(id, amount, PaymentVoucherStatus.ISSUED, null);
    }
}
