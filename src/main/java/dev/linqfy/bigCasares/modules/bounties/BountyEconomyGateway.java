package dev.linqfy.bigCasares.modules.bounties;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;

public final class BountyEconomyGateway implements BountyEconomy {

    private final JavaPlugin plugin;
    private final Economy economy;

    public BountyEconomyGateway(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public static Economy resolveOrThrow(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            throw new IllegalStateException("Vault no esta instalado.");
        }

        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null || provider.getProvider() == null) {
            throw new IllegalStateException("Vault esta presente pero no hay proveedor de economia.");
        }

        return provider.getProvider();
    }

    @Override
    public double balance(UUID playerId) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
        return economy.getBalance(player);
    }

    @Override
    public void withdraw(UUID playerId, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            throw new IllegalStateException("No se pudo retirar dinero de la victima: " + response.errorMessage);
        }
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            throw new IllegalStateException("No se pudo pagar la bounty: " + response.errorMessage);
        }
    }

    @Override
    public String format(double amount) {
        try {
            return economy.format(amount);
        } catch (RuntimeException ex) {
            return "$" + String.format(Locale.US, "%.2f", amount);
        }
    }
}
