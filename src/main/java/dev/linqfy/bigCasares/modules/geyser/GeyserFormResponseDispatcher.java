package dev.linqfy.bigCasares.modules.geyser;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

final class GeyserFormResponseDispatcher {
    private final Consumer<Runnable> mainThreadExecutor;

    GeyserFormResponseDispatcher(Consumer<Runnable> mainThreadExecutor) {
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    }

    void dispatch(BedrockShopForm form, UUID playerId, int buttonIndex) {
        mainThreadExecutor.accept(() -> form.responseHandler().accept(playerId, buttonIndex));
    }
}
