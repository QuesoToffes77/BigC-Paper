package dev.linqfy.bigCasares.modules.shop;

import dev.linqfy.bigCasares.BigCasares;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.Locale;

public class ShopEconomyGateway {

    private final BigCasares plugin;
    private final Economy economy;

    public ShopEconomyGateway(BigCasares plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean has(double amount) {
        return false;
    }

    public boolean withdraw(double amount) {
        return false;
    }

    public boolean deposit(double amount) {
        return false;
    }

    public boolean has(Player player, double amount) {
        return economy != null && economy.getBalance(player) >= amount;
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String format(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format(Locale.US, "$%.2f", amount);
    }

    protected BigCasares plugin() {
        return plugin;
    }

    protected Economy economy() {
        return economy;
    }
}
