package dev.linqfy.bigCasares.modules.pveboss;

import java.util.UUID;

@FunctionalInterface
public interface BossAbilityEffectGateway {

    void execute(UUID bossInstanceId, BossAbilityDefinition ability, BossAbilityRuntime runtime);

    static BossAbilityEffectGateway ignored() {
        return (bossInstanceId, ability, runtime) -> {
        };
    }
}
