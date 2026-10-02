# Acid Rain Mobs Design (Extension)

## Status

**Approved extension, 2026-08-14.** This document is a *new* extension of the
original `2026-08-07-acid-rain-design.md`. The original design explicitly kept
mobs **out of scope** ("Bosses, mutated mobs, loot economy, quests, crafting,
claims, or progression systems" and "It does not introduce broad unused
interfaces for mobs, bosses, loot, or economy"). This extension is the approved
decision to add a small, self-contained mob family to the Acid Rain module.

The mobs are **not** part of `mobscaling`, `pveboss`, or any other module. They
belong exclusively to `dev.linqfy.bigCasares.modules.acidrain`, following the
same internal structure (pure domain services + thin Bukkit runtime + fail-safe
config loader) as the rest of the module.

## Objective

During a **TOXIC** Acid Rain storm players should regularly encounter a small
family of hostile "toxic fauna". Mobs must never spawn because of vanilla
weather alone: the spawn gate is exactly

```text
event active AND phase == ACTIVE AND rain type == TOXIC
```

The feature must be configurable, bounded (no infinite accumulation, no
thousands of entities, no per-tick spam), resilient across `/acidrain reload`,
and covered by pure domain tests in the same style as the existing module.

## Scope

Implemented:

- Three mob types (see below) with differentiated roles.
- Internal identification via `PersistentDataContainer` under the `bigcasares`
  namespace, independent of display names.
- A pure spawn gate keyed off the real runtime state (`AcidRainSnapshot`),
  never off vanilla weather.
- A logical scheduler that starts/stops with the TOXIC phase and cannot
  duplicate.
- Configurable frequency, attempts, global cap, radius, and per-type toggles.
- Scheduler lifecycle tied to `AcidRainRuntime` state transitions, including
  escalation (ACID -> TOXIC mid-storm) and automatic starts.

Out of scope (as originally approved; the model + loot items below were added
by the 2026-08-17 extension documented at the end):

- Custom models, textures, sounds, or resource-pack assets.
- Rewarding, loot tables, quests, or progression integration.
- Automatic despawning/purging of already-spawned mobs when the storm ends
  (mobs that are alive when TOXIC ends simply remain; they die or despawn
  naturally). Only *new spawns* stop.
- `mobscaling`, `pveboss`, or any other module: they are untouched.

## Architecture

All new code lives under `dev.linqfy.bigCasares.modules.acidrain`:

- `AcidRainMobType` - pure enum: the three mob types, their display labels and
  relative spawn weights.
- `AcidRainMobIdentity` (+ `AcidRainMobTagStore` interface) - pure
  identification logic and the `bigcasares:acidrain_mob*` namespace keys.
- `BukkitAcidRainMobTagStore` - thin adapter from a Bukkit
  `PersistentDataContainer` to the pure tag store (same pattern as
  `BukkitAcidRainBlockAccess`).
- `AcidRainMobSpawnService` - pure domain: scheduler lifecycle (start/stop),
  spawn gate, interval accumulation, capacity checks, and weighted type
  selection. No server dependency.
- `BukkitAcidRainMobFactory` - thin mapping from `AcidRainMobType` to vanilla
  `EntityType` bases and attribute/name configuration.
- `AcidRainMobSettings` - settings record (lives with the other settings
  records in `AcidRainSettings.java`).
- `AcidRainRuntime` - owns the mob spawn service, the single registered
  cadence task (`acid-rain-mobs`), active-mob tracking, and actual spawning.

The spawn service reads the same `AcidRainSnapshot` the runtime already
produces. It never duplicates event state: there is exactly one storm service,
one snapshot, one spawn service, and one cadence task per module lifecycle.

## Mob types

Small family of three, differentiated by role, not just name:

| Type | Role | Base entity | Health | Speed | Damage | Weight |
| --- | --- | --- | --- | --- | --- | --- |
| `CRAWLER` | Common melee skirmisher | Spider | 22 | boosted (~0.42) | vanilla (2-3) | 5 |
| `BRUTE` | Rare heavy bruiser | Husk | 60 | slowed (~0.16) | boosted (~8) | 2 |
| `SPITTER` | Ranged toxic attacker | Bogged | 20 | vanilla | vanilla (poison arrows) | 2 |

Weights are relative: with defaults, Crawler is the common encounter, Brute and
Spitter are uncommon. The three roles map one-to-one to the requested
"Toxic Crawler / Toxic Brute / Toxic Spitter" functions; labels are
`Crawler`, `Brute`, `Spitter` and display names are `Toxic <label>`.

`CRAWLER`, `BRUTE`, `SPITTER` are the only types. No other mob types are
introduced.

## Identification

Every spawned mob is tagged at spawn time through the Bukkit
`PersistentDataContainer` under the `bigcasares` namespace:

```text
bigcasares:acidrain_mob        -> byte 1 (is an Acid Rain mob)
bigcasares:acidrain_mob_type   -> "CRAWLER" | "BRUTE" | "SPITTER"
```

Identification does **not** depend on the visible name. The tag store
abstraction keeps the logic pure and unit-testable (fake store in tests), while
`BukkitAcidRainMobTagStore` adapts real `PersistentDataContainer`s. This
distinguishes: a normal mob (no tags), a Toxic Crawler, a Toxic Brute, and a
Toxic Spitter.

## Spawn conditions

The gate is checked against the real runtime snapshot on every cadence tick:

```text
INACTIVE         -> no
WARNING          -> no
ACTIVE + ACID    -> no
ACTIVE + TOXIC   -> yes
ACTIVE + CHEMICAL-> no
ENDING           -> no new spawns
```

The gate is enforced twice:

1. The runtime syncs the logical scheduler each cadence tick
   (`start()` only while `ACTIVE && TOXIC`; `stop()` otherwise), so
   escalation (ACID -> TOXIC), manual `start toxic`, automatic starts, manual
   `stop`, and ENDING all converge within one cadence (10 ticks).
2. `AcidRainMobSpawnService.cycleDecision` independently rejects any snapshot
   that is not `ACTIVE` or not `TOXIC`, and rejects while the scheduler is
   stopped - a lifecycle bug can never leak spawns into the wrong phase.

"Simply raining" is never a condition. The module's own storm state is the
single source of truth.

## Frequency and limits

Shipped defaults (configurable, see Configuration):

- `spawn-interval-ticks: 60` - one spawn attempt cycle every 3 seconds.
- `attempts-per-cycle: 2` - up to 2 candidates per cycle.
- `max-active: 20` - global cap of alive Acid Rain mobs across all affected
  worlds (hard ceiling, never exceeded).
- `radius: 24` - spawn attempts are placed within this distance of an online
  player in an affected world.

This produces a regular, visible presence during TOXIC (up to ~40 attempts per
minute, population capped at 20) while keeping the entity count bounded.
Safety limits are never removed: `max-active` is a hard cap enforced before
every cycle, attempts are reduced to the remaining capacity, and spawned mobs
are vanilla entities that despawn naturally.

## Behavior

- **Toxic Crawler** - frequent; fast melee skirmisher; moderate health; low
  per-hit damage. Spawns often.
- **Toxic Brute** - uncommon; slow, high health, high damage; spawns rarely.
- **Toxic Spitter** - uncommon; ranged combat; its shots are visually tied to
  toxicity (a Bogged fires poison arrows); moderate damage and lower
  resistance than Brute.

Attributes are applied through the standard Bukkit `Attribute` API. All three
are vanilla hostile mobs, so they naturally target nearby players. No new
combat systems are introduced.

## Appearance

The project already has resource-pack infrastructure, but it is oriented to
items and bosses (BetterModel entities) and the module deliberately owns no
custom entity models. Following the "no new resource-pack system just for
this" rule, mobs reuse **vanilla entity bases** (Spider, Husk, Bogged) with
`Toxic <label>` display names in dark green so they are visually identifiable
on the server. Their internal identity is the PDC tag, not the name.

## Lifecycle and scheduler

- The Bukkit cadence task `acid-rain-mobs` is registered **once** per module
  enable (10-tick cadence, owned by the module scope, cancelled on disable -
  no orphaned tasks).
- The logical scheduler (`AcidRainMobSpawnService`) starts and stops with the
  TOXIC phase. `start()` and `stop()` are idempot
  creates a second scheduler, and the elapsed interval resets on start so each
  TOXIC storm begins fresh.
- `/acidrain reload` calls `updateSettings` on the same service instance; the
  cadence task and scheduler are never re-registered, so spawns are not
  duplicated after a reload.
- When the storm leaves `ACTIVE + TOXIC` (ACID, CHEMICAL, ENDING, INACTIVE,
  WARNING, manual stop, shutdown), new spawns stop within one cadence.
  Already-spawned mobs are not force-killed; they remain and despawn or die
  naturally.

## Configuration

New section under the existing `acid-rain` root (no parallel config):

```yaml
acid-rain:
  mobs:
    enabled: true
    spawn:
      interval-ticks: 60
      attempts-per-cycle: 2
      max-active: 20
      radius: 24
    types:
      crawler: true
      brute: true
      spitter: true
```

Loader behavior follows the module's existing fail-safe rules:

- Negative or invalid spawn values add an error and the whole loaded config
  fails safe (damage/destruction/automatic/mobs disabled), consistent with the
  original design.
- Absurd positive values are clamped with a warning
  (interval <= 600 ticks, attempts <= 32, max-active <= 200, radius <= 64).
- `max-active: 0`, `attempts-per-cycle: 0`, `radius: 0`, or all types disabled
  mean no mob spawns (feature effectively off, still safe).
- Records normalize non-finite/negative values defensively so no calculation
  can propagate them.

## Cleanup

- `AcidRainRuntime.shutdown()` and manual `stop` stop the scheduler.
- The module scope cancels the cadence task on disable.
- The active-mob tracker prunes dead/removed entities on every cycle, so the
  cap always reflects live mobs and stale UUIDs cannot accumulate.

## Testing

Pure domain tests (no Mockito, no live server), mirroring the existing Acid
Rain tests:

- State/level gate: INACTIVE, WARNING, ACTIVE+ACID, ACTIVE+CHEMICAL and ENDING
  never spawn; ACTIVE+TOXIC spawns.
- Limits: `max-active` is never exceeded; attempts shrink to remaining
  capacity; interval gating; radius bounds.
- Identification: a normal entity is not an Acid Rain mob; Crawler, Brute and
  Spitter are each identified correctly.
- Scheduler: no duplication on double start; stop halts; TOXIC -> ENDING stops;
  a second TOXIC storm starts fresh; reload keeps a single scheduler and does
  not multiply attempts.
- Integration: automatic start with default level TOXIC reaches ACTIVE+TOXIC
  and spawns; automatic start with default ACID does not.
- Config: defaults, custom values, per-type toggles, negative fail-safe,
  clamping.

## Extension (2026-08-17): custom model and nitric acid loot

Two follow-up additions were implemented on top of the approved design:

### Shared BetterModel for all three types

A new `toxic_mob.bbmodel` + `toxic_mob.png` (64x64) are packaged under
`src/main/resources/bettermodel/models/` and installed through the existing
`BetterModelAssetInstaller.installToxicMobModel` (immediate + one delayed
pass, mirroring the PvE module). The model is a single toxic blob body with
protruding eyes and top bumps and two animations:

- `idle` (loop, 2s) - breathing bob/wobble, played at spawn.
- `attack` (hold, 0.6s) - squish, played once via `animateOnce` when the mob
  is hit, returning to idle.

`AcidRainRuntime.spawnMobAt` attaches the model to every spawned mob
(`JavaModelGateway.attach` + `scale` per type: Crawler 0.95, Brute 1.35,
Spitter 0.8), plays `idle`, and hides the vanilla base entity
(`setInvisible(true)`) so the blob is the only visual. The vanilla hitbox and
AI are untouched - invisible entities remain fully targetable. A failed
attach only logs a warning and keeps the vanilla mob visible.

### Nitric acid drops

Toxic mobs drop the catalog item `nitric_acid` (a `POTION`-based catalog
material with **no crafting recipe**) on death. `AcidRainMobListener`
identifies deaths/hits by the existing PDC tags; the runtime adds the item to
the vanilla drop list through `CustomItemRegistry.createItemStack`. The drop
roll is a pure helper (`AcidRainMobDrops`) and is configurable:

```yaml
acid-rain:
  mobs:
    drops:
      enabled: true
      chance-percent: 100
      min-amount: 1
      max-amount: 2
```

### Lifecycle

Model trackers are released when a mob dies (death listener), when its entity
is pruned as dead/removed, and on module shutdown. Mobs that survive past the
TOXIC phase keep their models (the tracker map is deliberately NOT cleared on
storm end, so surviving mobs never become invisible vanilla entities). Tests:
`ToxicMobModelAssetTest` (asset/structure/installer contract), `AcidRainMobDropTest`
(pure roll), and `AcidRainMobSettingsTest` (drops config parsing/clamping).

## Verification

- `./gradlew test` (full suite) and `./gradlew clean build` pass.
- Live-server verification of an automatic TOXIC storm is performed on the
  server by the operator; the automated integration test simulates the full
  automatic WARNING -> ACTIVE(TOXIC) flow at the domain level.

## Visual Redesign - 2026-10-02

This revision supersedes the earlier shared-body visual description. Stable
BetterModel identifiers remain unchanged: `toxic_mob` (crawler), `toxic_brute`
(zombie), and `toxic_spitter` (skeleton). No gameplay, configuration, spawning,
dependency, or runtime changes are required.

- Crawler: low armored silhouette, eight independently animated legs, paired
  fangs, four eyes, and a restrained acid sac. 42 cubes and 14 bones.
- Brute: corroded shoulder armor, torn clothing, asymmetric acid wound, boots,
  and articulated limbs/head/jaw. 38 cubes and 9 bones.
- Spitter: open ribcage, aged bone, small acid core, corroded bow, and a distinct
  bow-draw attack pose. 51 cubes and 10 bones.

All retain `idle`, `walk`, `attack`, and `hurt`. The shared 128x128 material atlas
uses muted skin, bone, cloth, metal, and chitin with acid accents instead of
uniform neon noise. `resourcepack/sources/acid_mob_materials.png` is the authored
atlas; `python tools/gen_toxic_mob.py` deterministically generates all six
packaged model/texture files without modifying the atlas. UVs stay within
material tiles; texture frame metadata is explicit. Assets remain below 64
cubes and 16 bones per creature.

`python tools/preview_acid_mobs.py` renders an asset review sheet; use
`--animation attack --time 0.2` to inspect attack poses. These are standalone
geometry/texture previews, not Minecraft screenshots. The existing installer
updates the three BetterModel assets and the existing resource-pack publisher
handles regenerated packs. A server restart and accepting the updated pack
are required for operator verification.

### Agent Completion Report

- Created: shared source atlas, `tools/preview_acid_mobs.py`, and
  `AcidMobVisualDesignTest.java`.
- Modified: `tools/gen_toxic_mob.py`, the three packaged `.bbmodel`/`.png` pairs,
  `ToxicMobModelAssetTest.java`, and this design document.
- Deleted files: none. Dependencies, commands, permissions, and config: unchanged.
- Regression coverage: articulated anatomy, animation references, unique and
  reachable bone/cube IDs, UV bounds, PNG dimensions, canonical texture
  metadata, visual complexity budgets, and existing installer contracts.
- Validation: asset previews inspected; `gradlew.bat --offline clean build`
  completed successfully with 960 tests, 0 failures, 0 errors, and 0 skipped.
  JAR: `build/libs/BigCasares-0.0.1.jar`. Pack archives generated by the existing
  build tasks; BetterModel mob content is merged by the runtime publisher.
- Limitation: no live Minecraft client/server verification. BetterModel's full
  parser initializes Bukkit runtime registries, so unit tests validate the
  packaged format/contracts without claiming a live-engine render.

## Reference-Matched Melted Creatures - 2026-10-02

The operator supplied three design sheets (`esqueleto.png`, `zombie.png`, and
`arana.png`). This revision replaces the armored/industrial direction above
with their melted-organic direction. Reference images are design inputs, not
textures loaded directly by Minecraft.

- Zombie: moss-green skin, exposed half-skull and chest ribs, turquoise torn
  shirt, purple trousers, raw wounds, and hanging yellow acid. No armor/tank.
- Skeleton: warm ivory bones, fractured skull with a recessed acid cavity,
  open ribs, wooden bow, and acid on skull, hands, ribs, knees, and bow.
- Spider: red exposed flesh, ivory skull-like face, bone leg segments, exposed
  underside ribs, and hanging golden acid instead of armored purple chitin.

The final authored atlas is
`resourcepack/sources/acid_mob_melted_materials.png` (128x128, nearest-neighbor
normalization of a generated texture). It was created with the built-in image
tool, using the supplied sheets as material references, then refined to remove
painted cavities from bone tiles and simplify the pixel grid. Prompt summary:
"Flat 4x4 Minecraft albedo atlas: rotten moss skin, crimson flesh, clean ivory
bone, turquoise cloth, golden-yellow acid, dark cavities, purple trousers;
large deliberate pixels, no labels, perspective, lighting, or borders."
The earlier source atlas remains available but is no longer consumed by the
generator. Embedded texture bytes match the corresponding packaged PNGs.

Hanging acid is geometry attached to the existing bones, including droplets
and accumulations; it is not a new particle/task system. Final geometry budgets:
96/14 cubes/bones for crawler, 78/9 for zombie, and 86/10 for skeleton.
Only model data, textures, asset tooling, tests, and documentation changed.
The existing four animation IDs and three model keys are unchanged.

### Agent Completion Report - Reference Revision

- Created: `resourcepack/sources/acid_mob_melted_materials.png`.
- Modified: generator, preview renderer, all three model/texture pairs,
  `AcidMobVisualDesignTest.java`, and this document. No files deleted.
- Added regressions for hanging acid, absence of mechanical tanks/armor, and
  reference-specific exposed anatomy. Geometry ceiling is now 96 cubes while
  retaining at most 16 bones. Tests first failed on the previous assets and
  passed after regeneration.
- Preview renderer now supports front/back/left/right/top/bottom review using
  `--view`; front, back, three-quarter, and attack previews inspected.
- Deterministic regeneration verified: all six packaged asset hashes unchanged.
- Dependencies, gameplay, configs, commands, permissions: unchanged.
- Final verification: `gradlew.bat --offline clean build` succeeded; 962 tests
  passed, 0 failures, 0 errors, and 0 skipped. Both resource-pack archives were
  rebuilt. JAR: `build/libs/BigCasares-0.0.1.jar`, also copied to the existing
  test server with backup and SHA256 equality verification.
- Live Minecraft rendering remains unverified; previews are asset renders,
  not screenshots from Minecraft or proof of shader emission. Golden acid is
  colored geometry, not a promised shader-dependent glow effect.
