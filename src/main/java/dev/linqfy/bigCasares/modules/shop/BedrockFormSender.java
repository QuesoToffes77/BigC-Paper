package dev.linqfy.bigCasares.modules.shop;

import java.util.UUID;
import java.util.function.IntConsumer;

@FunctionalInterface
public interface BedrockFormSender {
    boolean send(UUID playerId, ShopView view, IntConsumer responseHandler);
}
