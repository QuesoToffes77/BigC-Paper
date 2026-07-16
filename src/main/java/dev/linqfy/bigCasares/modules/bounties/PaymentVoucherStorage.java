package dev.linqfy.bigCasares.modules.bounties;

import java.util.Optional;
import java.util.UUID;

public interface PaymentVoucherStorage {

    Optional<PaymentVoucher> load(UUID voucherId);

    void save(PaymentVoucher voucher);
}
