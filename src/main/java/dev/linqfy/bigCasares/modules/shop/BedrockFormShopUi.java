package dev.linqfy.bigCasares.modules.shop;

import java.util.UUID;
import java.util.function.Consumer;

public final class BedrockFormShopUi implements ShopUiGateway {
    private final BedrockFormSender sender;
    private final Consumer<ShopPurchaseRequest> purchaseHandler;
    private final BedrockShopFormMapper mapper = new BedrockShopFormMapper();

    public BedrockFormShopUi(BedrockFormSender sender, Consumer<ShopPurchaseRequest> purchaseHandler) {
        this.sender = sender;
        this.purchaseHandler = purchaseHandler;
    }

    @Override
    public void openShop(UUID playerId, ShopView shop) {
        sender.send(playerId, shop, index -> purchaseHandler.accept(mapper.purchaseForButton(playerId, shop, index)));
    }
}
