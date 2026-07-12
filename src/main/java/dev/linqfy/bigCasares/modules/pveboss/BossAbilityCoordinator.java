package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class BossAbilityCoordinator {

    private final UUID bossInstanceId;
    private final List<BossAbilityDefinition> abilities;
    private final Map<String, BossAbilityDefinition> abilitiesById;
    private final Map<String, Instant> cooldowns;
    private final BossAbilityEffectGateway effectGateway;
    private final BossTelegraphGateway telegraphGateway;
    private final BossTargetSelectionService targetSelectionService;
    private final RandomGenerator random;
    private final BossBehaviourCategoryWeight categoryWeight;

    private BossAbilityRuntime activeRuntime;
    private BossAbilityRuntime lastRuntime;
    private boolean disabled;

    public BossAbilityCoordinator(
        UUID bossInstanceId,
        List<BossAbilityDefinition> abilities,
        BossAbilityEffectGateway effectGateway,
        BossTelegraphGateway telegraphGateway
    ) {
        this(bossInstanceId, abilities, effectGateway, telegraphGateway, new BossTargetSelectionService(RandomGenerator.getDefault()), RandomGenerator.getDefault(), new BossBehaviourCategoryWeight(Map.of(), 0.05));
    }

    // For tests
    public BossAbilityCoordinator(
        UUID bossInstanceId,
        List<BossAbilityDefinition> abilities,
        BossAbilityEffectGateway effectGateway,
        BossTelegraphGateway telegraphGateway,
        RandomGenerator random
    ) {
        this(bossInstanceId, abilities, effectGateway, telegraphGateway, new BossTargetSelectionService(random), random, new BossBehaviourCategoryWeight(Map.of(), 0.05));
    }

    public BossAbilityCoordinator(
        UUID bossInstanceId,
        List<BossAbilityDefinition> abilities,
        BossAbilityEffectGateway effectGateway,
        BossTelegraphGateway telegraphGateway,
        BossTargetSelectionService targetSelectionService,
        RandomGenerator random,
        BossBehaviourCategoryWeight categoryWeight
    ) {
        this.bossInstanceId = Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        Objects.requireNonNull(abilities, "abilities");
        this.effectGateway = Objects.requireNonNull(effectGateway, "effectGateway");
        this.telegraphGateway = Objects.requireNonNull(telegraphGateway, "telegraphGateway");
        this.targetSelectionService = Objects.requireNonNull(targetSelectionService, "targetSelectionService");
        this.random = Objects.requireNonNull(random, "random");
        this.categoryWeight = Objects.requireNonNull(categoryWeight, "categoryWeight");
        this.cooldowns = new HashMap<>();

        ArrayList<BossAbilityDefinition> ordered = new ArrayList<>(abilities);
        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("a coordinator must define at least one ability");
        }
        Set<String> ids = new HashSet<>();
        for (BossAbilityDefinition ability : ordered) {
            Objects.requireNonNull(ability, "ability");
            if (!ids.add(ability.id())) {
                throw new IllegalArgumentException("duplicate ability id: " + ability.id());
            }
        }
        ordered.sort(Comparator.comparingInt(BossAbilityDefinition::priority).reversed());
        this.abilities = List.copyOf(ordered);
        HashMap<String, BossAbilityDefinition> definitions = new HashMap<>();
        this.abilities.forEach(ability -> definitions.put(ability.id(), ability));
        this.abilitiesById = Map.copyOf(definitions);
    }

    public Optional<BossAbilityRuntime> tick(Instant now, BossAbilityContext context) {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(context, "context");
        if (disabled) {
            return Optional.empty();
        }

        if (activeRuntime != null) {
            if (now.isBefore(activeRuntime.completesAt())) {
                telegraphGateway.update(
                    bossInstanceId,
                    abilitiesById.get(activeRuntime.abilityId()),
                    activeRuntime,
                    Duration.between(now, activeRuntime.completesAt())
                );
                return Optional.of(activeRuntime);
            } else {
                completeActiveCast(context);
                return Optional.ofNullable(lastRuntime);
            }
        }

        categoryWeight.normalizeWeights();

        BossAbilityDefinition selectedAbility = null;
        double highestScore = -1.0;
        Set<UUID> selectedTargets = Set.of();

        for (BossAbilityDefinition ability : abilities) {
            if (!isCooldownReady(ability.id(), now)) {
                continue;
            }

            if (!conditionsPass(ability, context, now)) {
                continue;
            }

            Set<UUID> targets = targetSelectionService.select(ability.targetSelector(), context);
            if (targets.isEmpty() && ability.targetSelector().maxTargets() > 0 && ability.targetSelector().radius() < Double.POSITIVE_INFINITY) {
                continue;
            }

            double score = ability.priority() * categoryWeight.getWeight(ability.category());
            if (score > highestScore) {
                highestScore = score;
                selectedAbility = ability;
                selectedTargets = targets;
            }
        }

        if (selectedAbility != null) {
            Duration castTime = selectedAbility.castTime();
            if (context.phase() == 3) {
                castTime = Duration.ofMillis((long) (castTime.toMillis() * 0.8));
            }
            if (castTime.isZero() || castTime.isNegative()) {
                activeRuntime = new BossAbilityRuntime(selectedAbility.id(), BossAbilityState.EXECUTING, now, now, selectedTargets);
                completeActiveCast(context);
                return Optional.ofNullable(lastRuntime);
            } else {
                BossAbilityRuntime casting = new BossAbilityRuntime(
                    selectedAbility.id(),
                    BossAbilityState.CASTING,
                    now,
                    now.plus(castTime),
                    selectedTargets
                );
                activeRuntime = casting;
                lastRuntime = casting;
                telegraphGateway.begin(bossInstanceId, selectedAbility, casting);
                return Optional.of(casting);
            }
        }
        return Optional.empty();
    }

    public Optional<BossAbilityRuntime> activeCast() {
        return Optional.ofNullable(activeRuntime);
    }

    public Optional<BossAbilityRuntime> lastRuntime() {
        return Optional.ofNullable(lastRuntime);
    }

    public boolean interruptCast(Instant now) {
        Objects.requireNonNull(now, "now");
        if (activeRuntime == null) {
            return false;
        }
        BossAbilityDefinition ability = abilitiesById.get(activeRuntime.abilityId());
        if (!ability.interruptible()) {
            return false;
        }
        cancelActiveCast(ability, BossAbilityState.INTERRUPTED);
        return true;
    }

    public boolean cancelCast(Instant now) {
        Objects.requireNonNull(now, "now");
        if (activeRuntime == null) {
            return false;
        }
        cancelActiveCast(abilitiesById.get(activeRuntime.abilityId()), BossAbilityState.INTERRUPTED);
        return true;
    }

    public void cancelAll(Instant now) {
        Objects.requireNonNull(now, "now");
        if (activeRuntime != null) {
            cancelActiveCast(abilitiesById.get(activeRuntime.abilityId()), BossAbilityState.DISABLED);
        }
        cooldowns.clear();
        disabled = true;
    }

    public boolean isCooldownReady(String abilityId, Instant now) {
        Objects.requireNonNull(abilityId, "abilityId");
        Objects.requireNonNull(now, "now");
        if (!abilitiesById.containsKey(abilityId)) {
            throw new IllegalArgumentException("unknown ability id: " + abilityId);
        }
        Instant cooldownUntil = cooldowns.get(abilityId);
        return cooldownUntil == null || !now.isBefore(cooldownUntil);
    }

    public Duration cooldownRemaining(String abilityId, Instant now) {
        if (isCooldownReady(abilityId, now)) {
            return Duration.ZERO;
        }
        return Duration.between(now, cooldowns.get(abilityId));
    }

    public boolean isDisabled() {
        return disabled;
    }

    public UUID bossInstanceId() {
        return bossInstanceId;
    }

    private void completeActiveCast(BossAbilityContext context) {
        BossAbilityDefinition ability = abilitiesById.get(activeRuntime.abilityId());
        BossAbilityRuntime executing = activeRuntime.withState(BossAbilityState.EXECUTING);
        effectGateway.execute(bossInstanceId, ability, executing);
        telegraphGateway.complete(bossInstanceId, ability, executing);

        double cooldownMultiplier = (context != null && context.phase() == 3) ? 0.7 : 1.0;
        Duration cooldown = Duration.ofMillis((long) (ability.cooldown().toMillis() * cooldownMultiplier));

        if (ability.comboChance() > 0 && ability.nextComboAbility() != null && !ability.nextComboAbility().isEmpty()) {
            if (random.nextDouble() < ability.comboChance()) {
                cooldowns.put(ability.nextComboAbility(), executing.completesAt());
            }
        }

        cooldowns.put(ability.id(), executing.completesAt().plus(cooldown));
        categoryWeight.reduceWeight(ability.category());

        lastRuntime = executing.withState(BossAbilityState.COOLDOWN);
        activeRuntime = null;
    }

    private void cancelActiveCast(BossAbilityDefinition ability, BossAbilityState state) {
        BossAbilityRuntime cancelled = activeRuntime.withState(state);
        activeRuntime = null;
        lastRuntime = cancelled;
        telegraphGateway.cancel(bossInstanceId, ability, cancelled);
    }

    private boolean conditionsPass(BossAbilityDefinition ability, BossAbilityContext context, Instant now) {
        for (BossAbilityCondition condition : ability.conditions()) {
            boolean result = switch (condition.type()) {
                case HEALTH_BELOW -> context.healthRatio() < condition.value();
                case HEALTH_ABOVE -> context.healthRatio() > condition.value();
                case PHASE_EQUALS -> context.phase() == (int) condition.value();
                case PLAYERS_AT_LEAST -> context.eligiblePlayerCount() >= (int) condition.value();
                case TARGET_IN_RANGE -> hasTargetInRange(context, condition.value());
                case NOT_CASTING -> activeRuntime == null;
                case COOLDOWN_READY -> isCooldownReady(ability.id(), now);
                case RANDOM_CHANCE -> random.nextDouble() < condition.value();
            };
            if (!result) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasTargetInRange(BossAbilityContext context, double range) {
        double rangeSquared = range * range;
        return context.targets().stream()
            .filter(BossTargetCandidate::eligible)
            .anyMatch(candidate -> candidate.position().distanceSquared(context.bossPosition()) <= rangeSquared);
    }
}
