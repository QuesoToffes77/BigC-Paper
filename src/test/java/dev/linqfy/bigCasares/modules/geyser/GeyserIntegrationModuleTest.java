package dev.linqfy.bigCasares.modules.geyser;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeyserIntegrationModuleTest {

    @Test
    void keepsJavaPathOperationalWhenGeyserIsAbsent() {
        GeyserPlatformGateway gateway = new GeyserPlatformGateway(Optional.empty());

        assertEquals(ClientPlatform.JAVA, gateway.resolvePlatform(UUID.randomUUID()));
        assertFalse(gateway.capabilities().available());
        assertFalse(gateway.capabilities().forms());
        assertFalse(gateway.capabilities().customEntities());
    }

    @Test
    void resolvesBedrockOnlyFromTheGeyserSessionRegistry() {
        UUID bedrock = UUID.randomUUID();
        GeyserApiFacade facade = new GeyserApiFacade() {
            @Override
            public boolean isBedrockPlayer(UUID playerId) {
                return playerId.equals(bedrock);
            }

            @Override
            public GeyserCapabilities capabilities() {
                return new GeyserCapabilities(true, true, true, true, false);
            }
        };
        GeyserPlatformGateway gateway = new GeyserPlatformGateway(Optional.of(facade));

        assertEquals(ClientPlatform.BEDROCK, gateway.resolvePlatform(bedrock));
        assertEquals(ClientPlatform.JAVA, gateway.resolvePlatform(UUID.randomUUID()));
        assertTrue(gateway.capabilities().resourcePacks());
    }

    @Test
    void exposesStableOptionalModuleId() {
        assertEquals("geyser-integration", new GeyserIntegrationModule(null).getId());
    }

    @Test
    void closesTheActiveRuntimeFacadeExactlyOnceOnDisable() {
        AtomicInteger closes = new AtomicInteger();
        GeyserApiFacade facade = new GeyserApiFacade() {
            @Override
            public boolean isBedrockPlayer(UUID playerId) {
                return false;
            }

            @Override
            public GeyserCapabilities capabilities() {
                return GeyserCapabilities.unavailable();
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
        GeyserIntegrationModule module = new GeyserIntegrationModule(null, facade);

        module.onDisable();
        module.onDisable();

        assertEquals(1, closes.get());
    }
}
