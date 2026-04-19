package dev.linqfy.bigCasares.modules.missions;

import java.util.UUID;

public interface RewardGateway {

    void deposit(UUID playerId, double amount);
}
