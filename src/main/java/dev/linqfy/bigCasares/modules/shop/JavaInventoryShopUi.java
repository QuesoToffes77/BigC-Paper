package dev.linqfy.bigCasares.modules.shop;

import java.util.UUID;
import java.util.function.Consumer;

public final class JavaInventoryShopUi implements ShopUiGateway {
    private final Consumer<UUID> opener;

    public JavaInventoryShopUi(Consumer<UUID> opener) {
        this.opener = opener;
    }

    @Override
    public void openShop(UUID playerId, ShopView shop) {
        opener.accept(playerId);
    }
}
