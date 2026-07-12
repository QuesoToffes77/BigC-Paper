package dev.linqfy.bigCasares.modules.pveboss;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record BossAbilityEffectDefinition(
    BossAbilityEffectType type,
    Map<String, Object> parameters
) {

    public BossAbilityEffectDefinition {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(parameters, "parameters");
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
        parameters.forEach((key, value) -> {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("effect parameter keys must not be blank");
            }
            copy.put(key, Objects.requireNonNull(value, "effect parameter value"));
        });
        parameters = Map.copyOf(copy);
    }

    public static BossAbilityEffectDefinition of(BossAbilityEffectType type) {
        return new BossAbilityEffectDefinition(type, Map.of());
    }

    public static BossAbilityEffectDefinition withNumber(
        BossAbilityEffectType type,
        String name,
        double value
    ) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("effect value must be finite");
        }
        return new BossAbilityEffectDefinition(type, Map.of(name, value));
    }

    public static BossAbilityEffectDefinition withText(
        BossAbilityEffectType type,
        String name,
        String value
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("effect text must not be blank");
        }
        return new BossAbilityEffectDefinition(type, Map.of(name, value));
    }
}
