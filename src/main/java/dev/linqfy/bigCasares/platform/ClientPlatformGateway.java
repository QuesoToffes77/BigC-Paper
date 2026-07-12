package dev.linqfy.bigCasares.platform;

import java.util.UUID;

@FunctionalInterface
public interface ClientPlatformGateway {
    ClientPlatform resolvePlatform(UUID playerId);
}
