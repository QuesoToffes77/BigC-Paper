package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Duration;
import java.util.UUID;

public interface BossTelegraphGateway {

    default void begin(
        UUID bossInstanceId,
        BossAbilityDefinition ability,
        BossAbilityRuntime runtime
    ) {
    }

    default void update(
        UUID bossInstanceId,
        BossAbilityDefinition ability,
        BossAbilityRuntime runtime,
        Duration remaining
    ) {
    }

    default void complete(
        UUID bossInstanceId,
        BossAbilityDefinition ability,
        BossAbilityRuntime runtime
    ) {
    }

    default void cancel(
        UUID bossInstanceId,
        BossAbilityDefinition ability,
        BossAbilityRuntime runtime
    ) {
    }
}
