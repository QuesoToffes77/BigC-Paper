package dev.linqfy.bigCasares.modules.geyser;

public final class GeyserCustomEntityGateway implements BedrockEntityGateway {
    private final GeyserApiFacade facade;

    public GeyserCustomEntityGateway(GeyserApiFacade facade) {
        this.facade = facade;
    }

    @Override
    public boolean register(GeyserCustomEntityDefinition definition) {
        return facade.capabilities().customEntities() && facade.registerCustomEntity(definition);
    }
}
