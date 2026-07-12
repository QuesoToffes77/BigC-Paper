package dev.linqfy.bigCasares.modules.shop;

import java.util.Objects;
import java.util.UUID;

public record ShopPurchaseRequest(UUID playerId, String shopId, String entryId) {
    public ShopPurchaseRequest {
        Objects.requireNonNull(playerId, "playerId");
        if (shopId == null || shopId.isBlank() || entryId == null || entryId.isBlank()) {
            throw new IllegalArgumentException("shopId and entryId cannot be blank");
        }
    }
}
