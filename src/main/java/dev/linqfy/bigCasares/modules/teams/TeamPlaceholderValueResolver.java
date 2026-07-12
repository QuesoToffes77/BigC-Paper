package dev.linqfy.bigCasares.modules.teams;

import java.util.UUID;

@FunctionalInterface
public interface TeamPlaceholderValueResolver {

    String resolve(UUID playerId, String placeholder);
}
