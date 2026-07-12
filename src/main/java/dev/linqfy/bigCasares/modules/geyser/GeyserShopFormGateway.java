package dev.linqfy.bigCasares.modules.geyser;

import java.util.UUID;

public final class GeyserShopFormGateway {
    private final GeyserApiFacade facade;

    public GeyserShopFormGateway(GeyserApiFacade facade) {
        this.facade = facade;
    }

    public boolean show(UUID playerId, BedrockShopForm form) {
        return facade.capabilities().forms() && facade.sendForm(playerId, form);
    }
}
