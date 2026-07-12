package dev.linqfy.bigCasares.modules.shop;

import java.util.UUID;

@FunctionalInterface
public interface ShopUiGateway {
    void openShop(UUID playerId, ShopView shop);
}
