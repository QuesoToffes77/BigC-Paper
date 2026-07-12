package dev.linqfy.bigCasares.modules.shop;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;

class ShopPlatformUiResolverTest {

    @Test
    void resolvesJavaBedrockAndUnknownFallbacks() {
        ShopUiGateway javaUi = (player, view) -> { };
        ShopUiGateway bedrockUi = (player, view) -> { };
        ShopUiGateway chatUi = (player, view) -> { };
        UUID player = UUID.randomUUID();

        assertSame(javaUi, new ShopPlatformUiResolver(id -> ClientPlatform.JAVA, javaUi, bedrockUi, chatUi).resolve(player));
        assertSame(bedrockUi, new ShopPlatformUiResolver(id -> ClientPlatform.BEDROCK, javaUi, bedrockUi, chatUi).resolve(player));
        assertSame(chatUi, new ShopPlatformUiResolver(id -> ClientPlatform.UNKNOWN, javaUi, bedrockUi, chatUi).resolve(player));
    }
}
