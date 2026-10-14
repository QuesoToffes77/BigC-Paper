---
name: add-boss-ability
description: Use when adding a new ability or behaviour to an existing PvE boss in BigCasares.
---

# Adding a Boss Ability

`BossAbilityCoordinator` selects YAML abilities by weighted priority, cooldowns, targets, and conditions.

Use `AbyssGuardianDefinitionLoader` as the schema reference. JAR deployment preserves existing `plugins/BigCasares/bosses/` YAML; update it explicitly when testing content changes.

## 1. YAML Definition Structure

Add an entry under `abilities:` in `src/main/resources/bosses/<boss-id>.yml`, adapting [void-pulse](../../../src/main/resources/bosses/abyss-guardian.yml). Set `category` (`PHYSICAL`, `MAGIC`, `MOVEMENT`, `DEFENSIVE`, `PASSIVE`), `animation`, `cooldown-seconds`, `cast-time-seconds`, `priority`, `interruptible`, conditions, targets, telegraph, and effects. Optional combo fields are explained below.

## 2. Conditions

All conditions must pass.
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

Types available: `NEAREST_PLAYER`, `FARTHEST_PLAYER`, `RANDOM_PLAYER`, `HIGHEST_DAMAGE`, `LOWEST_HEALTH`, `ALL_IN_RADIUS`, `RANDOM_POSITION`, `CURRENT_TARGET`.

`RANDOM_POSITION` currently selects a random player's UUID, not a ground position. `ALL_IN_RADIUS` ignores YAML `max-targets` and selects every eligible candidate in range.

Example:
```yaml
    targets:
      type: NEAREST_PLAYER
      radius: 15
      max-targets: 1
```

## 4. Telegraph

Schema enum: `CIRCLE`, `CONE`, `LINE`, `TARGET_MARK`, `GROUND_MARK`, `COUNTDOWN`, `BOSS_ANIMATION`, `SOUND_WARNING`. The current `Telegraphs` renderer ignores type/particle and draws a purple dust circle plus warning sound for every type. Supply a telegraph section: `BossAbilityDefinition` requires it.

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

`PARTICLE` currently uses Witch particles; `PARTICLE_SHAPE` supports `CIRCLE` and `SPIRAL`. The `SAHUR_*` effects require `PaperSahurCombatController`. Other enum entries (e.g. `TELEPORT`, `SUMMON`, `HEAL`) currently fall through the generic runtime without an effect.

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

- `combo-chance` and `next-combo-ability` can reset the named ability's cooldown on completion. Normal conditions, weighted selection, and cast time still apply; this does not force an instant cast.
- During Phase 3 (Enraged), all ability cast times are reduced by 20%, and all cooldowns are reduced by 30%. Keep this pacing in mind when balancing.

## 7. Implementation Notes

- New effect behavior needs an enum/loader contract and runtime dispatch in `executeEffects` (or the Sahur controller), with tests and transient cleanup.
- If you add a new telegraph type, you **must** implement its rendering in the `Telegraphs` inner class of `PaperAbyssGuardianRuntime.java`.
- Ensure the animation referenced by `animation:` exists in the boss's `animations:` section and has the same name in the boss's BetterModel `.bbmodel` source. Reload BetterModel before testing the ability in-game.
- Run the relevant `pveboss` tests and `bash gradlew clean build`; test both clients. Bedrock cast/phase notification currently stores names in memory without transmitting state to controllers.
