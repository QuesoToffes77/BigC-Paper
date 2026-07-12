package dev.linqfy.bigCasares.modules.geyser;

public final class GeyserCustomItemGateway {
    private final GeyserApiFacade facade;

    public GeyserCustomItemGateway(GeyserApiFacade facade) {
        this.facade = facade;
    }

    public boolean register(GeyserCustomItemDefinition definition) {
        return facade.capabilities().customItems() && facade.registerCustomItem(definition);
    }
}
