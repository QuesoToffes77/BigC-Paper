package dev.linqfy.bigCasares.modules.shop;

import java.util.UUID;

public final class BedrockShopFormMapper {
    public ShopPurchaseRequest purchaseForButton(UUID playerId, ShopView view, int buttonIndex) {
        if (buttonIndex < 0 || buttonIndex >= view.items().size()) {
            throw new IllegalArgumentException("Invalid Bedrock shop button index: " + buttonIndex);
        }
        return new ShopPurchaseRequest(playerId, view.id(), view.items().get(buttonIndex).id());
    }
}
