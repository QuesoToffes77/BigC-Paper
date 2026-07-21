# Tracker Compass and Nuke Shot Design

## Objective

Add two custom combat items to BigCasares: a reusable Tracker Compass that temporarily follows a struck player, and a single-use Nuke Shot fishing rod that creates a cinematic falling TNT-ring formation.

## Scope

### In scope

- Register both items through the existing custom-item catalog and registry.
- Add one self-contained `special-items` module for item behavior and runtime cleanup.
- Track a struck player for 45 seconds, followed by a 60-second attacker cooldown.
- Update the Tracker Compass name, lore, compass target, and enchantment glint while tracking.
- Show remaining cooldown in the Tracker Compass lore without glint.
- Consume the Nuke Shot on right-click and suppress normal fishing-rod casting.
- Animate a seed TNT display into ten horizontal concentric rings centered 50 blocks above the user.
- Allocate no more than 130 TNT positions across all ten rings, with each successive ring containing more TNT than the previous ring.
- Build one ring every five ticks, hold the formation motionless during buildup, and release all TNT together after 2.5 seconds.
- Clean active tracking state, scheduled work, displays, and not-yet-released formations when the module disables or reloads.
- Add Java and Bedrock item appearance metadata using repository conventions. New textures are limited to available project assets or safe placeholders if no dedicated art is supplied.

### Out of scope

- Persistent tracking or cooldown state across server restarts.
- Tracking targets across worlds.
- Remote targeting, target selection menus, or tracking without a melee hit.
- Custom TNT explosion physics, block-damage protection, or altered blast power.
- Crafting recipes unless separately specified.
- Changes to existing custom items or unrelated modules.

## Confirmed Gameplay Rules

### Tracker Compass

- The base material is `COMPASS`.
- The holder activates it by dealing a melee hit to another player while holding the custom compass in the attacking hand.
- Activation starts 45 seconds of tracking for that attacker and target.
- While active, the compass points to the target's current location, has enchantment glint, and displays the target name and remaining tracking time in its name and lore.
- The display and compass target refresh once per second.
- A hit during active tracking does not replace the target or restart the timer.
- After tracking ends, the attacker receives a 60-second cooldown. During cooldown the compass has no glint and its lore displays the remaining cooldown.
- A hit during cooldown does not activate tracking or restart the cooldown.
- If the target disconnects, dies, or leaves the attacker's world, tracking ends early and the full cooldown begins.
- Tracking and cooldown belong to the attacker, preventing bypass by moving, dropping, or replacing compass stacks.
- All Tracker Compass stacks in the attacker's inventory are reconciled to the attacker's current state. A compass outside that inventory returns to its catalog-default appearance.

### Nuke Shot

- The base material is `FISHING_ROD` with one maximum durability point exposed through the catalog item definition.
- A main-hand right-click activates the item, cancels vanilla rod casting, and consumes the held Nuke Shot immediately.
- The formation center is the user's X/Z position at activation and exactly 50 blocks above the user's Y position at activation.
- Activation begins with one stationary TNT block display at the center.
- Ten concentric horizontal rings appear from smallest to largest, one every five ticks.
- Ring populations are strictly increasing and total exactly 130 TNT positions. Points are distributed evenly by angle around each ring.
- The seed display is an animation precursor and is removed as the first ring appears; it does not add a 131st falling TNT.
- All display entities remain stationary until the tenth ring is complete.
- After the tenth ring appears, every display is replaced with a normal primed TNT entity at the same position. All TNT entities begin falling on the same tick and use the normal server TNT fuse and explosion behavior.
- If the world becomes unavailable or the module disables before release, the unfinished display formation is removed without spawning TNT.
- Once real TNT has been released, it behaves as normal world TNT and is not removed by module shutdown.

## Architecture

### Module and item definitions

- `SpecialItemsModule` owns registration, listeners, scheduler lifecycle, and cleanup. Its stable module id is `special-items`.
- `TrackerCompassItem` and `NukeShotItem` extend the existing catalog-backed custom-item abstraction.
- Catalog YAML files define identity, material, display text, model data, stack size, and appearance metadata.

### Tracker components

- `TrackerCompassService` contains Bukkit-free timing and transition rules for idle, tracking, and cooldown states. Time is supplied by callers so tests are deterministic.
- `TrackerCompassState` records attacker, target, tracking deadline, and cooldown deadline without mutable global state.
- `TrackerCompassListener` detects qualifying melee hits and player lifecycle events, then delegates transitions to the service.
- `TrackerCompassRuntime` performs the once-per-second Bukkit reconciliation: resolves online players, updates compass targets, and rebuilds matching inventory item metadata from catalog defaults plus current state.

### Nuke components

- `NukeRingLayout` is Bukkit-free geometry logic that calculates ten rings with strictly increasing populations totaling 130 and evenly spaced positions.
- `NukeShotListener` validates main-hand right-click activation, consumes exactly one item, cancels the vanilla interaction, and delegates animation startup.
- `NukeAnimationRuntime` spawns the seed and ring displays, schedules five-tick expansion steps, atomically converts the final formation to primed TNT, and owns pre-release cleanup.
- A small runtime entity gateway separates layout/timeline decisions from Bukkit entity creation so meaningful behavior can be tested without a server.

## State and Data Flow

1. A Tracker Compass melee hit asks the service to start tracking.
2. The service accepts only an idle attacker and records a 45-second deadline.
3. The once-per-second runtime resolves the target and applies the active item presentation and compass direction.
4. Expiry or target invalidation transitions the attacker into a fresh 60-second cooldown.
5. Cooldown expiry removes attacker state and restores inventory compasses to their catalog appearance.
6. A Nuke Shot right-click removes the item before animation startup, preventing duplicate activation.
7. The animation runtime snapshots the center, renders the seed, and adds one calculated ring per five ticks.
8. The final step replaces all 130 displays with falling primed TNT in one scheduler tick.

## Configuration

```yaml
modules:
  special-items: true

special-items:
  tracker-compass:
    tracking-seconds: 45
    cooldown-seconds: 60
    update-ticks: 20
  nuke-shot:
    height: 50
    ring-count: 10
    total-tnt: 130
    ring-interval-ticks: 5
```

The confirmed values are defaults and remain configuration-backed for server tuning. Validation rejects non-positive durations, ring counts, heights, and totals that cannot produce strictly increasing nonempty rings.

## Error Handling and Safety

- Item activation requires registry identity, not material or display-name matching.
- Interaction handling is main-hand only to prevent duplicate Bukkit hand events.
- The Nuke Shot is consumed before any entities are scheduled or spawned.
- A failed animation spawn removes any displays already created and reports the failure through the plugin logger.
- Layout generation enforces the total cap before runtime spawning.
- Tracker presentation is reconstructed from the catalog item rather than repeatedly appending lore or enchantments.
- Module shutdown cancels owned tasks and removes only pre-release display entities created by this module.

## Testing

- `SpecialItemsModuleWiringTest` verifies the stable module id.
- `TrackerCompassServiceTest` covers activation, active-hit rejection, exact tracking expiry, early invalidation, cooldown rejection, and cooldown expiry.
- `NukeRingLayoutTest` verifies ten rings, strictly increasing populations, exactly 130 unique positions, increasing radii, and even angular distribution.
- Listener behavior stays thin; testable activation guards and item-consumption calculations are extracted where useful.
- Catalog/resource tests verify both definitions, model identifiers, one-item stack limits, and Nuke Shot durability metadata.
- Focused module tests run before the complete Gradle test suite and build.

## Boundaries

- Do not change the pinned Minecraft, Java, Gradle, or dependency versions.
- Do not modify existing custom-item behavior.
- Do not alter global TNT explosion rules or protection systems.
- Do not persist per-player state.
- Do not run `gradle clean`; the existing runtime state under `build/run-server` must remain intact.
- Preserve unrelated work already present in the dirty worktree.
