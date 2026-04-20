package dev.linqfy.bigCasares.modules.bounties;

import java.util.UUID;

public interface BountyEconomy {

    double balance(UUID playerId);

    void withdraw(UUID playerId, double amount);

    void deposit(UUID playerId, double amount);

    String format(double amount);
}
