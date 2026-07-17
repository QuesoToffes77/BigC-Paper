package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class YamlPaymentVoucherStorage implements PaymentVoucherStorage {

    private final Path directory;

    public YamlPaymentVoucherStorage(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<PaymentVoucher> load(UUID voucherId) {
        Path file = fileFor(voucherId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        try {
            double amount = config.getDouble("amount");
            PaymentVoucherStatus status = PaymentVoucherStatus.valueOf(config.getString("status", ""));
            String claimant = config.getString("claimed-by");
            UUID claimedBy = claimant == null || claimant.isBlank() ? null : UUID.fromString(claimant);
            return Optional.of(new PaymentVoucher(voucherId, amount, status, claimedBy));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Vale de pago inválido: " + voucherId, ex);
        }
    }

    @Override
    public void save(PaymentVoucher voucher) {
        try {
            Files.createDirectories(directory);
            YamlConfiguration config = new YamlConfiguration();
            config.set("amount", voucher.amount());
            config.set("status", voucher.status().name());
            config.set("claimed-by", voucher.claimedBy() == null ? null : voucher.claimedBy().toString());
            config.save(fileFor(voucher.id()).toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar el vale de pago " + voucher.id(), ex);
        }
    }

    private Path fileFor(UUID voucherId) {
        return directory.resolve(voucherId + ".yml");
    }
}
