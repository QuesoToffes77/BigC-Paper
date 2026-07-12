package dev.linqfy.bigCasares.modules.shop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedrockShopFormMapperTest {

    @Test
    void mapsButtonToTheSharedPurchaseRequest() {
        UUID player = UUID.randomUUID();
        ShopView view = new ShopView("weapons", "Armas", "Elegí un objeto", "$1500",
            List.of(new ShopViewItem("sword", "Espada", "§710 daño", "$250", null, true)));

        ShopPurchaseRequest request = new BedrockShopFormMapper().purchaseForButton(player, view, 0);

        assertEquals(new ShopPurchaseRequest(player, "weapons", "sword"), request);
    }
}
