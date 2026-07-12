package dev.linqfy.bigCasares.modules.geyser;

import java.nio.file.Files;
import java.nio.file.Path;

public final class GeyserResourcePackGateway {
    private final GeyserApiFacade facade;

    public GeyserResourcePackGateway(GeyserApiFacade facade) {
        this.facade = facade;
    }

    public boolean register(Path pack) {
        return facade.capabilities().resourcePacks() && Files.isRegularFile(pack)
            && facade.registerResourcePack(pack);
    }
}
