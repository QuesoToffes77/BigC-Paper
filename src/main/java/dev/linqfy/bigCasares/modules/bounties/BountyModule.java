package dev.linqfy.bigCasares.modules.bounties;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class BountyModule implements PluginModule {

    private final BigCasares plugin;

    private BountyEconomyGateway economyGateway;
    private BountyService service;
    private PaymentVoucherService paymentVouchers;
    private PaymentPaper paymentPaper;
    private RuntimeRegistrationScope compatibilityScope;

    public BountyModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "bounty-system";
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        Economy economy = BountyEconomyGateway.resolveOrThrow(plugin);
        BountySettings settings = new BountySettings(
            plugin.getConfig().getDouble("bounty-system.economy.take-percent", 0.30),
            plugin.getConfig().getDouble("bounty-system.economy.bounty-percent", 0.20),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-balance", 100.0),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-bounty", 25.0)
        );

        Path playersDirectory = plugin.getDataFolder().toPath().resolve("data").resolve("bounties").resolve("players");
        BountyStorage storage = new YamlBountyStorage(playersDirectory);
        this.economyGateway = new BountyEconomyGateway(plugin, economy);
        this.service = new BountyService(storage, economyGateway, settings, Instant::now, victimId ->
            plugin.getDangerModule() != null && plugin.getDangerModule().service()
                .map(danger -> danger.snapshot(victimId).tier() == dev.linqfy.bigCasares.modules.danger.DangerTier.LETAL)
                .orElse(false),
            killerId -> {
                if (plugin.getDangerModule() == null) return 1.0;
                return plugin.getDangerModule().service()
                    .map(danger -> {
                        dev.linqfy.bigCasares.modules.danger.DangerTier tier = danger.snapshot(killerId).tier();
                        if (tier == dev.linqfy.bigCasares.modules.danger.DangerTier.PELIGROSO) return 1.1; // +10%
                        if (tier == dev.linqfy.bigCasares.modules.danger.DangerTier.LETAL) return 1.3;     // +30%
                        return 1.0;
                    })
                    .orElse(1.0);
            }
        );
        this.paymentVouchers = new PaymentVoucherService(
                new YamlPaymentVoucherStorage(plugin.getDataFolder().toPath().resolve("data").resolve("payments")),
                economyGateway
        );
        this.paymentPaper = new PaymentPaper(plugin);
        scope.register("module-state", this::clearRuntimeState);
        registrations.registerListener("bounty-listener", new BountyListener(service, economyGateway));
        registrations.registerListener(
            "payment-paper-listener",
            new PaymentPaperListener(paymentPaper, paymentVouchers, economyGateway)
        );
        BountyCommand commands = new BountyCommand(this);
        for (String commandName : List.of("bounty", "bal", "withdraw")) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                registrations.bindCommand(commandName + "-command", command, commands, commands);
            }
        }
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
    }

    public BountyEconomy economy() {
        return economyGateway;
    }

    public BountyService service() {
        return service;
    }

    public BigCasares plugin() {
        return plugin;
    }

    public PaymentPaper paymentPaper() {
        return paymentPaper;
    }

    public PaymentVoucher withdrawToVoucher(UUID payerId, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("Payment amount must be finite and positive");
        }
        if (economyGateway.balance(payerId) < amount) {
            throw new IllegalArgumentException("Insufficient balance for payment");
        }
        economyGateway.withdraw(payerId, amount);
        try {
            return paymentVouchers.issue(amount);
        } catch (RuntimeException ex) {
            try {
                economyGateway.deposit(payerId, amount);
            } catch (RuntimeException refundFailure) {
                ex.addSuppressed(refundFailure);
            }
            throw ex;
        }
    }

    public List<BalanceStanding> topBalances(int limit) {
        if (limit < 1) {
            return List.of();
        }
        return Arrays.stream(plugin.getServer().getOfflinePlayers())
                .filter(player -> player.isOnline() || player.hasPlayedBefore())
                .map(this::balanceStanding)
                .filter(standing -> standing.balance() > 0.0)
                .sorted(Comparator.comparingDouble(BalanceStanding::balance).reversed())
                .limit(limit)
                .toList();
    }

    private BalanceStanding balanceStanding(OfflinePlayer player) {
        return new BalanceStanding(player.getUniqueId(), economyGateway.balance(player.getUniqueId()));
    }

    private void clearRuntimeState() {
        paymentPaper = null;
        paymentVouchers = null;
        service = null;
        economyGateway = null;
    }
}
