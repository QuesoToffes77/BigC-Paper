---
name: add-boss-ability
description: Use when adding a new ability or behaviour to an existing PvE boss in BigCasares. Covers ability definition, conditions, targets, effects, and telegraphs.
---

# Adding a Boss Ability

Boss abilities in BigCasares are data-driven and defined in the boss's YAML file. They are executed by the `BossAbilityCoordinator` based on priority, cooldowns, category weights, and conditions.

## 1. YAML Definition Structure

Add the new ability under the `abilities:` section in `src/main/resources/bosses/<boss-id>.yml`:

```yaml
  <ability-id>:
    category: <CATEGORY> # PHYSICAL, MAGIC, MOVEMENT, DEFENSIVE, PASSIVE
    animation: <animation-id> # References an animation defined in the animations section
    cooldown-seconds: 15
    cast-time-seconds: 2
    priority: 50
    interruptible: false
    conditions:
      # See Conditions section below
    targets:
      # See Targets section below
    telegraph:
      # See Telegraph section below
    effects:
      # See Effects section below
    combo-chance: 0.25 # (Optional) 25% chance to combo
    next-combo-ability: <ability-id> # (Optional) Next ability to cast instantly if combo triggers
```

## 2. Conditions

Conditions determine when the ability can be cast. All conditions must pass.
Types available: `HEALTH_BELOW`, `HEALTH_ABOVE`, `PHASE_EQUALS`, `PLAYERS_AT_LEAST`, `TARGET_IN_RANGE`, `NOT_CASTING`, `COOLDOWN_READY`, `RANDOM_CHANCE`.

Example:
```yaml
    conditions:
      - type: HEALTH_BELOW
        value: 0.5
      - type: NOT_CASTING
      - type: TARGET_IN_RANGE
        value: 10
```

## 3. Targets

The target selector defines who is affected by the ability.
Types available: `NEAREST_PLAYER`, `FARTHEST_PLAYER`, `RANDOM_PLAYER`, `HIGHEST_DAMAGE`, `LOWEST_HEALTH`, `ALL_IN_RADIUS`, `RANDOM_POSITION`, `CURRENT_TARGET`.

Example:
```yaml
    targets:
      type: NEAREST_PLAYER
      radius: 15
      max-targets: 1
```

## 4. Telegraph

Telegraphs warn players before the cast finishes.
Types available: `CIRCLE`, `CONE`, `LINE`, `TARGET_MARK`, `GROUND_MARK`, `COUNTDOWN`, `BOSS_ANIMATION`, `SOUND_WARNING`.

Example:
```yaml
    telegraph:
      type: CIRCLE
      radius: 5
      particle: minecraft:flame
      warning-sound: minecraft:entity.warden.attack_impact
```

## 5. Effects

Effects are executed when the cast finishes.
Types available (currently implemented in `PaperAbyssGuardianRuntime`): `DAMAGE`, `KNOCKBACK`, `PUSH`, `PULL`, `MESSAGE`, `SOUND`, `PARTICLE`, `PARTICLE_SHAPE`, `PROJECTILE`, `PULL_CONTINUOUS`, `POTION_EFFECT_AREA`, `SUMMON_CLONE`.

Example:
```yaml
    effects:
      - type: DAMAGE
        amount: 12
      - type: KNOCKBACK
        strength: 1.5
      - type: SOUND
        sound: minecraft:entity.warden.sonic_boom
      - type: PROJECTILE
        movement: TELEDIRECTED # LINEAR, GRAVITY, TELEDIRECTED
        speed: 1.5
        damage: 10
        turn-rate: 0.15 # Smoothness of homing
        bounces: 0
      - type: PULL_CONTINUOUS
        radius: 10
        pull-strength: 0.3
        damage: 5
        duration: 5.0
      - type: SUMMON_CLONE
        amount: 3
        duration: 10.0
```

## 6. Combos & Enrage Mode

- If you specify `combo-chance` (0.0 to 1.0) and `next-combo-ability` in an ability's YAML, the boss has a chance to instantly reset the cooldown of the next ability, forcing a combo.
- During Phase 3 (Enraged), all ability cast times are reduced by 20%, and all cooldowns are reduced by 30%. Keep this pacing in mind when balancing.

## 7. Implementation Notes

- If you add a new effect type (e.g., `POTION_EFFECT`, `SUMMON`), you **must** implement its logic in the `executeEffects` method of `PaperAbyssGuardianRuntime.java`.
- If you add a new telegraph type, you **must** implement its rendering in the `Telegraphs` inner class of `PaperAbyssGuardianRuntime.java`.
- Ensure the animation referenced by `animation:` exists in the boss's `animations:` section and has the same name in the boss's BetterModel `.bbmodel` source. Reload BetterModel before testing the ability in-game.
