# Blood Moon and Environmental Visuals Design

## Objective

Add `blood-moon` as a production BigCasares module and give the existing Acid Rain levels distinct, bounded visual identities without changing their gameplay balance.

## Architecture

The module follows `PluginModule`, `BukkitRuntimeRegistrations`, and `RuntimeRegistrationScope`.

- `BloodMoonModule`: settings, command/listener/task registration, lifecycle ownership.
- `BloodMoonRuntime`: per-world event orchestration, feedback, loaded-mob reconciliation, and cleanup.
- `BloodMoonNightController`: pure day/night transitions for chance and interval schedules.
- `BukkitBloodMoonMobModifier`: stable keyed Paper attribute modifiers.
- `BloodMoonSpawner`: loaded-chunk spawn attempts with pressure and distance checks.
- `BloodMoonSpawnPolicy`: pure distance, pressure, and event-cap policy.
- `EnvironmentalVisualService`: shared client-side vanilla particle profiles and hard particle budgets.

## Lifecycle and Scheduling

Each configured world has an independent `INACTIVE` or `ACTIVE` state. Time is evaluated as a range, not an exact tick, so `/time set`, sleep, and skipped values still produce safe transitions.

- Night is `13000 <= floorMod(fullTime, 24000) < 23000`.
- Chance mode evaluates once per world night.
- Interval mode counts world nights and starts every configured number.
- Minimum normal nights are enforced after a chance-based event.
- Sunrise stops the event and performs cleanup.
- Manual start is rejected during daytime, on duplicate start, in excluded worlds, and during conflicting environmental events.

Runtime state is intentionally not persisted because existing environmental events use runtime state. Stable attribute keys and entity-load reconciliation prevent duplicate or stale modifiers after reload/restart.

## Mob Modifiers

Default eligibility is Bukkit/Paper `Enemy`, excluding players and all passive entities. Configured blacklist always wins; an optional whitelist can include compatible custom hostile base entities.

The module adds only:

- `bigcasares:blood_moon_health`: `MULTIPLY_SCALAR_1`, amount `0.5` for `1.5x` max health.
- `bigcasares:blood_moon_speed`: `MULTIPLY_SCALAR_1`, amount `0.25` for `1.25x` movement speed.

Existing attribute base values and modifiers from Mob Scaling, equipment, bosses, or other plugins remain intact. Applying twice detects the stable key and does nothing. Cleanup removes only those two keys and clamps current health to the restored maximum.

Loaded hostiles are processed once at event start. New and chunk-loaded entities are reconciled through events. Players never receive these modifiers. Damage remains `1.0x` by default.

## Extra Spawning

Blood Moon does not alter permanent server spawn limits. Every configured interval it performs bounded attempts around eligible players.

Safety gates include:

- non-peaceful difficulty and active mob-spawn gamerules;
- existing loaded chunks only;
- configured minimum and maximum player distance;
- solid, non-liquid floor and passable feet/head blocks;
- low block light;
- hostile/spawnable entity types only;
- per-player and per-world extra-mob ledgers;
- a pressure ceiling derived from the world's vanilla monster limit and configured multiplier.

No work runs per block or per entity every tick. Loaded hostile pressure is counted only on the spawn cadence.

## Environmental Visual Profiles

All effects are sent directly to nearby players and do not mutate blocks, biomes, weather textures, or global resources.

- `ACID_RAIN`: bright acid-green vertical dust/falling-water streaks and small corrosive splashes.
- `TOXIC_SPORES`: small green/yellow dust motes mixed with slow spore-blossom particles.
- `CHEMICAL_FOG`: broad yellow-green dust volumes with sparse slow smoke.
- `BLOOD_MOON`: restrained dark-red and crimson dust ambience.

`LOW`, `MEDIUM`, and `HIGH` scale density under a hard maximum of 36 particles per player per visual cycle. Radius and cadence are configurable and clamped.

## Vanilla Client Limitations

A vanilla resource pack cannot dynamically select a different moon, rain texture, or true fog color only while a server-side event is active. Replacing `rain.png`, moon phases, shaders, or biome fog resources would affect normal weather/nights globally.

Therefore this implementation intentionally uses vanilla particle, title, BossBar, and sound APIs. No unused PNG assets were added, and normal rain, nights, moon phases, and biome fog remain unchanged.

## Cleanup

Sunrise, manual stop, world unload, reload, and plugin disable remove Blood Moon modifiers from loaded entities, remove BossBars, clear spawn ledgers, and stop owned tasks/listeners through the runtime scope. Entities loaded later are reconciled and stale stable-key modifiers are removed while the event is inactive.

## Testing

Tests cover scheduling, duplicate starts, cooldown nights, interval mode, health/speed multiplication, no stacking, selective cleanup, passive/player rejection, existing/new hostiles, spawn distances and caps, fail-safe configuration, module/plugin contracts, visual profile distinction, particle budgets, and preservation of vanilla resource paths.
