package dev.linqfy.bigCasares.modules.shop;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import dev.linqfy.bigCasares.platform.ClientPlatformGateway;

import java.util.Objects;
import java.util.UUID;

public final class ShopPlatformUiResolver {
    private final ClientPlatformGateway platformGateway;
    private final ShopUiGateway javaUi;
    private final ShopUiGateway bedrockUi;
    private final ShopUiGateway chatFallback;

    public ShopPlatformUiResolver(
        ClientPlatformGateway platformGateway,
        ShopUiGateway javaUi,
        ShopUiGateway bedrockUi,
        ShopUiGateway chatFallback
    ) {
        this.platformGateway = Objects.requireNonNull(platformGateway, "platformGateway");
        this.javaUi = Objects.requireNonNull(javaUi, "javaUi");
        this.bedrockUi = Objects.requireNonNull(bedrockUi, "bedrockUi");
        this.chatFallback = Objects.requireNonNull(chatFallback, "chatFallback");
    }

    public ShopUiGateway resolve(UUID playerId) {
        return switch (platformGateway.resolvePlatform(playerId)) {
            case JAVA -> javaUi;
            case BEDROCK -> bedrockUi;
            case UNKNOWN -> chatFallback;
        };
    }

    public void open(UUID playerId, ShopView view) {
        resolve(playerId).openShop(playerId, view);
    }
}
