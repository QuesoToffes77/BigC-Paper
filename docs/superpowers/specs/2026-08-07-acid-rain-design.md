# Acid Rain Design

## Objective

Add `acid-rain` as a production BigCasares environmental gameplay module. The event is owned by the module state, not by vanilla weather alone, and provides warning, active storm, escalation, recovery, player feedback, damage, protection, and controlled environmental erosion.

## Scope

Implemented:

- Explicit lifecycle states: `INACTIVE`, `WARNING`, `ACTIVE`, `ENDING`.
- Levels: `ACID`, `TOXIC`, `CHEMICAL`.
- Configurable duration, warning, escalation, automatic starts, worlds, damage, protection, sounds, particles, bossbar, actionbar, water flag, and environmental erosion.
- `/acidrain start [acid|toxic|chemical]`, `/acidrain stop`, `/acidrain reload`, `/acidrain info`.
- Permissions: `bigcasares.acidrain.admin`, `start`, `stop`, `reload`, `bypass`. Bypass is intentionally explicit and is not inherited from the Acid Rain admin permission.
- Player exposure cache with movement invalidation.
- Protection groups for full equipment sets and future carried/custom item hooks.
- Temporary contamination state cleared on stop/end/shutdown.
- Incremental block erosion queue with event and per-second rate limits.
- Hard-coded block safety barrier for leaves, ores, containers, crops, and special blocks.
- Cancelable `AcidRainBlockErodeEvent` before a block is changed.

Out of scope:

- Resource pack assets, custom textures, custom models, custom sounds, custom particles.
- Bosses, mutated mobs, loot economy, quests, crafting, claims, or progression systems.
- Permanent water transformation or ocean-scale conversion.

## Architecture

The module lives under `dev.linqfy.bigCasares.modules.acidrain` and follows the existing `PluginModule` plus `RuntimeRegistrationScope` pattern.

- `AcidRainModule`: module entry point, settings load/reload, command/listener/task registration.
- `AcidRainRuntime`: Bukkit-facing orchestration for lifecycle ticks, damage ticks, feedback ticks, environment ticks, bossbars, sounds, particles, weather snapshots, and cleanup.
- `AcidRainStormService`: pure domain lifecycle, durations, state transitions, automatic start cooldown, escalation, temporary contamination.
- `AcidRainDamageService`: pure domain damage eligibility, interval control, bypass/protection/shelter/world checks.
- `AcidRainEnvironmentService`: pure domain erosion queue and rate limiting.
- `AcidRainBlockSafetyPolicy`: pure domain whitelist-first erosion safety.
- `AcidRainSettingsLoader`: fail-safe config loader.
- `AcidRainExposureCache`: Bukkit exposure cache invalidated by movement/world/teleport/join/quit events.
- `AcidRainProtectionService`: Bukkit equipment protection resolver.
- `BukkitAcidRainBlockAccess`: final block validation and mutation bridge.

## Lifecycle

Allowed transitions:

```text
INACTIVE -> WARNING -> ACTIVE -> ENDING -> INACTIVE
```

Manual `/acidrain start` skips warning and starts at the configured default level unless a level is provided. Automatic starts use warning first. Duplicate starts are rejected.

Stop moves the event toward cleanup immediately and clears:

- damage intervals;
- erosion queue and event counters;
- exposure cache;
- bossbars;
- temporary contamination;
- weather snapshots.

Block erosion only happens while the state is `ACTIVE`. `WARNING` shows messages, sounds, bossbar and now particles (the same particle system used during the storm), but never mutates blocks; `ENDING` stops erosion and resets the environment queue, event counters and per-second budget. The environment service itself rejects any `processSecond` call whose storm is not `ACTIVE`, so a lifecycle bug can never leak destruction into the wrong phase.

## Level intensity

`ACID`, `TOXIC` and `CHEMICAL` are not just different labels: each level owns its own destruction tuning under `levels.<level>.destruction`, and every knob is validated and clamped.

```yaml
levels:
  acid:
    destruction:
      interval-ticks: 40      # a destruction cycle every 2 seconds
      candidates-per-cycle: 30
  toxic:
    destruction:
      interval-ticks: 20      # every 1 second
      candidates-per-cycle: 60
  chemical:
    destruction:
      interval-ticks: 10      # every 0.5 seconds
      candidates-per-cycle: 90
```

The environment task runs on a 10-tick cadence and the runtime only harvests/processes when the active level's interval has elapsed. Candidates per cycle and the cadence produce a real intensity ramp (roughly 15 / 60 / 90 blocks per second with the shipped defaults). The global safety caps stay untouched: `max-blocks-per-second` is enforced as a real per-second budget inside `AcidRainEnvironmentService` (shared across cycles, keyed by wall-clock second), and `max-blocks-per-event` caps the whole event. Higher levels therefore destroy more often and break more blocks, but never above the configured safety limits and never bypassing the whitelist.

If a level does not declare `candidates-per-cycle`, the global `environment.destruction.candidates-per-cycle` is used, so existing configurations keep working.

## Erosion radius invariant

Each queued candidate stores its origin and the radius that was active at harvest time. The environment service re-validates the distance at processing time and skips any candidate outside the radius, so blocks are never destroyed outside the configured radius even if the player moved or a stale candidate reaches the queue.

## Gameplay

Players get a warning window before automatic events. During active Acid Rain, damage occurs only when all required conditions hold:

- storm state is `ACTIVE`;
- player is in an affected world;
- player is online;
- player is exposed;
- player does not have bypass;
- protection is below 100 percent;
- level interval has elapsed.

Protection is clamped from 0 to 100 percent. The defaults model basic, advanced, and chemical full suits without assuming one single item equals full protection.

## Environment Safety

Erosion is whitelist-first:

```text
not in whitelist -> no erosion
```

Additional hard barriers reject leaves, ores, containers, crops by default, and special blocks even if a future config accidentally whitelists them. A blank whitelist means no destruction. The default whitelist includes stone, cobblestone, deepslate, logs, and planks so the event is visible in wooden test structures while still requiring explicit material allowance.

Block mutation path:

1. Candidate is queued from player-local loaded chunks only.
2. The runtime scans a small bounded vertical range from the surface and around the player height to find explicitly whitelisted materials that are actually reachable by the event.
3. Per-second and per-event limits are checked.
4. Current material is read.
5. Whitelist and safety barriers are evaluated.
6. Nexus protected containers are respected.
7. `AcidRainBlockErodeEvent` is fired and cancellation is honored.
8. Block becomes air only after all checks pass.

## Performance

The module avoids world-wide scans. It samples candidates near online players, stores them in a de-duplicated queue, and processes at most `max-blocks-per-second`. The production defaults use a concentrated radius and higher rate limit so erosion is noticeable during active storms without bypassing the whitelist. Exposure uses a short cache and movement invalidation rather than per-tick raycasts for every player.

Runtime tasks are registered once through `BukkitRuntimeRegistrations`:

- lifecycle task;
- damage task;
- feedback task;
- environment task.

The module scope owns every task, listener, command binding, and runtime cleanup.

## Configuration

Configuration root: `acid-rain`.

Dangerous invalid values fail safe. Invalid duration, negative limits, invalid chance, negative vertical scan depth, or invalid levels disable damage, environmental destruction, and automatic activation in the loaded settings. Invalid material names are ignored. Zero block limits mean no environmental destruction.

Non-finite numbers are handled defensively: `NaN`/`Infinity` damage, contamination or protection values are rejected by the loader, and the level settings record normalizes non-finite values to zero so no calculation can ever propagate them. Absurd destruction values are clamped with a warning instead of becoming unlimited: radius (max 64), candidates per cycle (max 512), max-blocks-per-second (max 120), max-blocks-per-event (max 10000), vertical scan depth (max 128). Sound volume and pitch are clamped to the 0-2 range when finite. The environment queue is capped (4096 candidates) and de-duplicated by block key to protect against lag spikes.

## Extensibility

The module leaves small natural extension points:

- `AcidRainBlockErodeEvent` for external protection systems.
- Protection groups with carried-material support for future custom items, filters, respirators, CustomModelData, or ItemModel.
- Level effect lists for future toxic materials or custom effect mapping.
- Separate storm, damage, environment, and runtime classes that can later support new events such as Black Storm or Radioactive Rain.

It does not introduce broad unused interfaces for mobs, bosses, loot, or economy.

## Testing

Pure domain tests cover lifecycle, levels, damage, config validation, environment safety, rate limits, cleanup, and module wiring. Bukkit live behavior is intentionally thin and delegated to the tested services where possible.
