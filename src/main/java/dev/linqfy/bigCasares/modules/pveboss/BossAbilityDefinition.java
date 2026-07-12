package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public record BossAbilityDefinition(
    String id,
    Duration cooldown,
    Duration castTime,
    BossTargetSelectorDefinition targetSelector,
    List<BossAbilityCondition> conditions,
    List<BossAbilityEffectDefinition> effects,
    BossTelegraphDefinition telegraph,
    boolean interruptible,
    int priority,
    String animationId,
    BossBehaviourCategory category,
    double comboChance,
    String nextComboAbility
) {

    public BossAbilityDefinition {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ability id must not be blank");
        }
        id = id.trim();
        Objects.requireNonNull(cooldown, "cooldown");
        Objects.requireNonNull(castTime, "castTime");
        Objects.requireNonNull(targetSelector, "targetSelector");
        Objects.requireNonNull(conditions, "conditions");
        Objects.requireNonNull(effects, "effects");
        Objects.requireNonNull(telegraph, "telegraph");
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("cooldown must not be negative");
        }
        if (castTime.isNegative()) {
            throw new IllegalArgumentException("castTime must not be negative");
        }
        conditions = List.copyOf(conditions);
        effects = List.copyOf(effects);
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("an ability must define at least one effect");
        }
    }
}
