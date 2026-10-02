# Grappling Hook (6 Tiers) — Design

**Status:** Approved extension
**Date:** 2026-08-14
**Module:** `grappling-hook` (new independent module)
**Related:** `docs/superpowers/specs/2026-08-07-acid-rain-design.md`,
`docs/superpowers/specs/2026-07-15-declarative-item-catalog-design.md`

## 1. Purpose

Add a progression of six Grappling Hook items. Each tier costs more to craft,
reaches farther, and cools down faster than the previous one. Using a hook
launches a visible hook that travels to the targeted block, extends a Minecraft
chain behind it, and then pulls the player toward the anchor point.

This is a **new extension**: it does not reuse Acid Rain, mob scaling, or PvE
bosses. Acid Rain keeps its own module; the Grappling Hook gets its own module
under `modules/grapplinghook` and its own `grappling-hook` config section. It
reuses the existing declarative item catalog (definitions, recipes, appearance
assets) and the existing Paper display-entity and ray-trace infrastructure.

## 2. Architecture

- **Module** `grappling-hook` (independent `PluginModule`), package
  `dev.linqfy.bigCasares.modules.grapplinghook`.
- **Item identity** lives in the declarative catalog: each tier is a catalog
  item (`grappling_hook_1` … `grappling_hook_6`) identified by the catalog
  persistent-data id (`bigcasares:item_id`). No parallel identity system.
- **Recipes** are declared in the catalog YAML (transactional recipe runtime of
  `custom-item-catalog`); tier `N+1` upgrades tier `N` with a new material.
- **Pure domain** (unit-testable, no Bukkit): tier definitions, settings,
  cooldown state, shot math, and the grapple simulation (travel/attach phases,
  chain sampling, impulse).
- **Bukkit runtime**: one 1-tick task owned by the module scope drives all
  active sessions. Visuals use `BlockDisplay` entities (the same display-entity
  pattern as `NukeAnimationRuntime`): a `TRIPWIRE_HOOK` block for the hook and
  `CHAIN` blocks for the chain.

### Files

| File | Responsibility |
| --- | --- |
| `GrapplingHookTier` | Enum I–VI: catalog id, model data, default range/cooldown |
| `GrapplingHookSettings` | Record + `load(FileConfiguration)` with clamps |
| `GrappleCooldownService` | Per-player wall-clock cooldown state (pure) |
| `GrappleShotMath` | Travel ticks, lerp, chain sampling, impulse vector (pure) |
| `GrappleSimulation` | Session phases TRAVEL → ATTACH → DONE (pure) |
| `GrapplingHookItem` | Catalog-backed item handle per tier |
| `GrapplingHookListener` | Right-click → raycast → session start |
| `GrapplingHookActivationMode` | Configurable input: clicks, sneaking variants, SWAP_HANDS (F) |
| `GrappleRaycast` | Deterministic voxel traversal + ray-vs-AABB (pure, orientation-independent) |
| `GrapplingHookRuntime` | 1-tick driver: hook + chain displays, pull, retract, cleanup |
| `GrapplingHookModule` | Module wiring (items, listener, task, scope) |

## 3. Tiers

| Tier | Catalog id | Model data | Range (blocks) | Cooldown (s) |
| --- | --- | --- | --- | --- |
| I | `grappling_hook_1` | 1012 | 12 | 5.00 |
| II | `grappling_hook_2` | 1013 | 16 | 4.25 |
| III | `grappling_hook_3` | 1014 | 20 | 3.50 |
| IV | `grappling_hook_4` | 1015 | 25 | 2.75 |
| V | `grappling_hook_5` | 1016 | 30 | 2.00 |
| VI | `grappling_hook_6` | 1017 | 36 | 1.25 |

All values are defaults; each tier's `range` and `cooldown-seconds` can be
overridden under `grappling-hook.tiers.<n>` in `config.yml`.

## 4. Use flow

1. Activate with the configured input (right/left click, sneaking variants,
   or F via `SWAP_HANDS`). One input produces exactly one session.
2. A session always starts on activation: a `TRIPWIRE_HOOK` display travels
   from the eye toward the target while `CHAIN` displays extend behind it
   (one every `chain.spacing` blocks, recomputed every tick).
3. Targeting is ray traced from the player's eye along the full camera
   direction with a deterministic voxel traversal (walls, floors, ceilings,
   diagonals all behave the same) plus a ray-vs-AABB entity test; the nearest
   valid target wins, capped at the tier range. A miss flies to the end of
   the range and cleans up.
4. On arrival the chain goes tense between the player and the target and the
   pull impulse fires once (plus a small upward bias). The session ends when
   the player arrives, or after the attach budget, never leaving orphan
   displays.
5. Re-activating while a session is active retracts the hook: it returns to
   the player, the chain disappears progressively, and the session ends
   without a pull.
6. Chain and hook displays are removed, the tier cooldown starts (also shown
   with the vanilla item-cooldown bar), and the session ends.

## 5. Safety

- One active session per player; new uses while a session is in flight are
  ignored.
- Cooldown is enforced server-side (wall clock) and re-shown client-side.
- Misses (no block hit) apply a short `fail-cooldown-ticks` so the mechanic
  cannot be spammed every tick.
- Display count is bounded by `range / chain-spacing` (60 at the default
  spacing with the max tier range); sessions are capped by the number of online players.
- `close()` on module disable removes every display and clears all sessions and
  cooldowns — no orphan entities or tasks survive reload.

## 6. Configuration

```yaml
grappling-hook:
  enabled: true
  activation:
    mode: RIGHT_CLICK        # RIGHT_CLICK | LEFT_CLICK | SNEAK_RIGHT_CLICK | SNEAK_LEFT_CLICK | SWAP_HANDS (F)
    retract-on-activation: true
  chain:
    enabled: true
    spacing: 1.0
  hook-speed: 1.6
  attach-ticks: 4
  fail-cooldown-ticks: 10
  targeting:
    blocks: true
    entities: true
    players: false
  tiers:
    i:   { range: 50.0,  cooldown-seconds: 5.0,  impulse-power: 2.0, upward-bias: 0.25 }
    ii:  { range: 75.0,  cooldown-seconds: 4.25, impulse-power: 2.4, upward-bias: 0.28 }
    iii: { range: 100.0, cooldown-seconds: 3.5,  impulse-power: 2.8, upward-bias: 0.31 }
    iv:  { range: 125.0, cooldown-seconds: 2.75, impulse-power: 3.3, upward-bias: 0.35 }
    v:   { range: 150.0, cooldown-seconds: 2.0,  impulse-power: 3.8, upward-bias: 0.40 }
    vi:  { range: 200.0, cooldown-seconds: 1.25, impulse-power: 4.2, upward-bias: 0.45 }
```

Legacy keys still load: top-level `chain-spacing` (→ `chain.spacing`) and
`entities.enabled` / `entities.allow-players` (→ `targeting`). An unknown
`activation.mode` falls back to `RIGHT_CLICK` with a console warning.

Activation is one input → one session. Re-activating while a session is
active retracts the hook (when `retract-on-activation` is true) instead of
firing a second shot. `SWAP_HANDS` consumes the vanilla `F` key via
`PlayerSwapHandItemsEvent` (cancelled only when the hook is in hand).

Numeric sanitation: ranges are clamped to `[1, 256]`, cooldowns to
`[0.25, 120]` s, spacing to `[0.25, 4]`, hook speed to `[0.1, 8]`, attach ticks
to `[0, 40]`, fail cooldown to `[0, 200]`, per-tier impulse power to
`[0.25, 4.0]` blocks/tick and per-tier upward bias to `[0, 1]`. The final
pull velocity is always clamped to `4.5` blocks/tick
(`GrapplingHookSettings.MAX_IMPULSE_SPEED`), so no configuration can launch a
player at a dangerous speed.

The pull impulse is `normalize(target - eye) * impulse-power` plus a small
`upward-bias` on the Y axis (an arc assist that never dominates the direction
toward the target), clamped to the speed ceiling. While the chain stays tense
a sustained winch pull is applied every attach tick (50% of the tier power,
capped at 1.8 blocks/tick) so the pull keeps feeling strong instead of dying
out after one tick. Against an entity the hooked target is attracted toward
the player (capped at 1.4 blocks/tick) instead of pulling the player. Higher
tiers pull harder (2.0 → 4.2 blocks/tick, roughly doubling from tier I to
VI) while range and cooldown stay unchanged.

## 7. Recipes (progressive)

All recipes are shaped and declared in the catalog. Tier `N+1` consumes tier
`N` (exact custom-item ingredient) plus a new material:

| Tier | Upgrade material | Result |
| --- | --- | --- |
| I | IRON_INGOT + LEAD + FISHING_ROD (base) | `grappling_hook_1` |
| II | GOLD_INGOT + `grappling_hook_1` | `grappling_hook_2` |
| III | DIAMOND + `grappling_hook_2` | `grappling_hook_3` |
| IV | EMERALD + `grappling_hook_3` | `grappling_hook_4` |
| V | NETHERITE_INGOT + `grappling_hook_4` | `grappling_hook_5` |
| VI | NETHER_STAR + `grappling_hook_5` | `grappling_hook_6` |

## 8. Tests

- Tier table: six tiers, unique ids/model data, escalating range, descending
  cooldown, `fromCatalogId`.
- Settings: defaults, per-tier overrides, clamps.
- Cooldown: start/expire/clear, per-player isolation.
- Shot math: travel ticks from distance/speed, lerp endpoints, chain sampling
  (spacing, count, final anchor), impulse direction and upward bias.
- Simulation: TRAVEL → ATTACH → DONE, progressive chain growth, impulse fired
  exactly once, per-player session isolation, cleanup.
- Catalog: the six definitions parse and their recipes resolve every referenced
  ingredient inside the catalog.

## 9. Out of scope

- No new resource-pack system: hooks use the vanilla fishing-rod item model and
  the existing pack pipeline for appearance assets.
- No new command: `/bigcasares give <id>` already covers catalog items.
- No damage system, no entity grappling, no wall-piercing, no multi-target.

## 10. Enhancement: sounds, feedback and entity grappling

Approved follow-up (same module, no architectural change):

- **Sounds** (vanilla only, per-event configurable under `grappling-hook.sounds`):
  fire = `BLOCK_PISTON_EXTEND`, chain tick = `BLOCK_CHAIN_STEP` throttled to
  `chain-interval-ticks` (never every tick), attach = `BLOCK_CHAIN_HIT`,
  impulse = `BLOCK_CHAIN_PLACE`, fail = `BLOCK_CHAIN_BREAK`, cooldown =
  `UI_BUTTON_CLICK`. Unknown sound names fall back to the default at load time.
  `GrappleFeedbackPlan` scales volume/pitch slightly per tier (3% per step) so
  higher tiers feel a bit more powerful without exaggeration.
- **Visual feedback** (one-shot bursts only, behind `feedback.enabled`):
  a small CRIT burst at launch, an IRON_BLOCK spark burst at the attach point,
  and a small CLOUD puff at the player on pull. The chain stays the main
  animation; counts default to 6 and are capped at 32.
- **Entity grappling** (behind `entities.enabled`, players only with
  `allow-players`): the ray now tests blocks and valid entities and keeps the
  nearest hit, so an entity behind a wall is never grabbed. Denied kinds:
  the shooter, players by default, `Display`/`Interaction` (chain, hook,
  holograms), `Projectile`, `Marker`, and anything dead or invalid. Entity
  shots follow the target every tick (hook, chain and pull recompute the live
  attach point); if the target dies or vanishes the session is cancelled and
  every display is removed. Movement stays the existing impulse — no teleport.

## 11. Chain Rendering Maintenance (2026-10-02)

- The rope uses non-persistent vanilla `IRON_CHAIN` block displays, not particles,
  placed blocks, or custom fullbright assets. Client packs that replace the vanilla
  iron-chain model/texture also affect this rope. Shader appearance depends on the
  client's shader support for display entities; the plugin does not install shaders.
- Each segment is centered on the midpoint between consecutive sample points.
  Its local Y axis rotates toward the next point and scales to the exact segment
  length. This removes vertical floating segments and the half-block offset.
- Chain sampling has a hard limit of 96 displays per session. Dense/long chains
  increase effective spacing while still reaching the hook. Short chains keep
  the configured spacing. Non-finite geometry is rejected.
- Display view range is `1.5` (a multiplier, not a block distance). Position
  interpolation takes one tick and lighting remains ambient.
- Offline/dead owners or dimension changes cancel the session. Unexpected
  per-session runtime errors clean up that session instead of aborting all grapples.
- Ranges, recipes, hand activation, movement physics, and item-model transforms
  are unchanged by this maintenance patch.

### Companion Copper Apple Fix

- Validate cooldown/consumption at `HIGH`, but grant effects and commit the
  cooldown at `MONITOR` only for a non-cancelled custom-item consumption event.
  A protection/inventory plugin cancelling at `HIGHEST` cannot grant free effects
  or consume the cooldown. The clock is injectable for deterministic tests.
- Lore lists the actual effect levels/durations and identifies lightning as visual,
  without changing balance. Existing apples receive the canonical lore on refresh.
- The oxidation timer remains in the action bar, not in changing per-second lore.
  Persistent age, oldest-age merge behavior, identity, and maximum stack size are
  preserved; this patch does not claim identical metadata for different-age apples
  before the existing merge normalization runs.

### Verification

Regression tests cover bounded sampling, invalid spacing, endpoint alignment in
horizontal/vertical/diagonal directions, consumption cancellation, cooldown expiry,
other foods, spectator mode, logout cleanup, and canonical effect descriptions.
Minecraft shader/visual rendering still requires a real-client check.

## Agent Completion Report (2026-10-02 Maintenance)

- Modified: `GrappleShotMath`, `GrapplingHookRuntime`, `CopperAppleConsumeListener`,
  `CopperAppleOxidationStage`, `CopperAppleOxidationService`, their geometry/balance
  tests, and this document. Added `CopperAppleConsumeListenerTest`. Deleted no files.
- No dependency, configuration, recipe, range, hand-transform, or resource-pack
  source changes. Existing Java/Bedrock pack generation remains enabled.
- Initial regression run: 32 tests, 3 expected failures (display budget,
  non-finite spacing, consumption handler priority). Focused post-fix tests passed.
- Final command: `gradlew.bat --offline clean build` (all tests enabled).
  Result: BUILD SUCCESSFUL; 955 tests passed, 0 failed, 0 errors, 0 skipped.
- JAR: `build/libs/BigCasares-0.0.1.jar`.
  Pack: `build/generated-resourcepacks/bigcasares-java.zip`.
- A locked previous Gradle test-output directory was preserved by moving `build`
  to a timestamped `build-stale-*` directory before retrying the initial tests.
  The final clean build completed normally; no files were force-deleted.
- Real server/client and shader appearance: NOT TESTED. Automated tests verify
  geometry and consumption rules, not visual quality in Minecraft.
