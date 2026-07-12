package dev.linqfy.bigCasares.modules.geyser;

import dev.linqfy.bigCasares.platform.ClientPlatform;
import dev.linqfy.bigCasares.platform.ClientPlatformGateway;

import java.util.Optional;
import java.util.UUID;

public final class GeyserPlatformGateway implements ClientPlatformGateway {
    private final Optional<GeyserApiFacade> facade;

    public GeyserPlatformGateway(Optional<GeyserApiFacade> facade) {
        this.facade = facade;
    }

    @Override
    public ClientPlatform resolvePlatform(UUID playerId) {
        return facade.filter(api -> api.isBedrockPlayer(playerId)).isPresent()
            ? ClientPlatform.BEDROCK
            : ClientPlatform.JAVA;
    }

    public GeyserCapabilities capabilities() {
        return facade.map(GeyserApiFacade::capabilities).orElseGet(GeyserCapabilities::unavailable);
    }
}
