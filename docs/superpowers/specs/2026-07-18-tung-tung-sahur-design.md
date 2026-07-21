# Tung Tung Tung Sahur Boss Design

## Goal

Add Tung Tung Tung Sahur as a second PvE boss without removing or regressing the existing Abyss Guardian. Sahur is an aggressive arena boss for 10-20 players, tuned for an approximately eight-minute fight. The implementation uses the supplied Sahur, summoned-bat, Bedrock, reward-item, and `sakur.png` assets.

## Architecture

The shared PvE boss module continues to own boss definition loading, instance lifecycle, health UI, damage accounting, presentation, music, and cleanup. Boss definitions are selected by ID, and `/boss spawn tung-tung-sahur` creates Sahur while `/boss spawn abyss-guardian` remains supported.

Sahur receives a dedicated combat controller behind focused, testable policies for:

- participant and health scaling;
- wall-aware charge direction scoring;
- charge victim mounting and release;
- frantic movement and anti-melee pressure;
- summoned-bat targeting and formations;
- damage ranking and reward delivery.

Every boss-owned entity and scheduled task is attached to one boss instance. Defeat, module shutdown, invalid entities, and failed casts all close their owned tasks, model handles, carriers, bats, displays, and temporary state.

## Raid Scaling

Eligible participants are online, alive, non-spectator players in the boss audience radius when Sahur spawns. The participant count is clamped to 10-20 and is fixed for that instance so healing or reconnecting cannot change maximum health mid-fight.

Maximum health is:

```text
20,000 + 4,000 * clamped participant count
```

This yields 60,000 health for 10 or fewer eligible players and 100,000 health for 20 or more. Ability damage does not scale with player count. Later phases increase cadence and pursuit pressure rather than producing unavoidable damage spikes.

## Movement and Attacks

### Frantic Walk

Outside committed attacks, Sahur uses the supplied `walk` animation and aggressively navigates the arena. It changes its movement target or lateral bias every 20-60 ticks, strongly preferring reachable player clusters while avoiding stationary target-lock behavior.

### Wall Charge

The charge dynamically uses physical arena geometry. It samples 24 evenly spaced horizontal directions plus direct bearings toward current player clusters, ray-traces each direction to the first solid collision, and accepts solid-wall paths between 8 and 40 blocks long. Each accepted corridor scores eligible players whose horizontal position is within 2.25 blocks of its segment. Sahur telegraphs for 24 ticks and commits to the highest-scoring corridor, resolving equal scores in favor of the shorter wall path.

During the charge, the boss uses `sprint`. Each collided player is mounted on a separate invisible saddled-pig carrier owned by the boss instance. Carriers follow the charge without accepting normal AI or damage. On physical wall impact, interruption, invalidation, or cleanup, all victims are dismounted safely. A normal wall impact deals 16 raw damage and outward knockback. Cleanup paths release victims without applying wall-impact damage.

### Bat Hit Combo

The supplied `bat_hit` animation produces a forward melee hit for 14 raw damage. A completed hit has a 35 percent chance to chain a second swing; the second has a 15 percent chance to chain a final third swing. The combo is capped at three attacks and reevaluates the forward hit volume for each swing.

### Spin Attack

The supplied `spin_in_place` animation is mapped to the `spin-attack` ability. It becomes more likely when at least four players crowd within four blocks or Sahur receives four close-range critical hits within two seconds. Sahur moves unpredictably for 80 ticks. Contact applies 5 raw damage and outward knockback at most once per player every 10 ticks.

### Stomp

The supplied `stomp` animation creates an eight-block shockwave rendered with brown dust particles. Players caught by it take 6 raw damage and strong radial knockback. The telegraph and expanding particle ring make the affected radius readable before impact.

## Summoned Bats

All summoned bats use `bat_boss.bbmodel`, the supplied Bedrock bat files, and `sakur.png`. They are temporary boss-owned entities rather than normal persistent mobs.

### Hunter Bat

One bat rises from the ground using `rise_type_throw`, then loops `spin_type_throw`. It initially selects the farthest eligible player and bounces through up to four distinct targets, preferring the farthest valid next target after each hit. Each target has a hit cooldown, and the bat expires after its route or timeout.

### Shield Bats

Five bats using `type_shield` form a small moving arc between Sahur and the direction producing the greatest number of player-fired arrows during the previous five seconds. For 160 ticks, the formation follows Sahur and intercepts player-fired `AbstractArrow` projectiles that cross it. Intercepted projectiles are consumed; the bats are visuals and blockers, not independently damageable mobs.

### Launch Bat

A bat appears below a randomly selected eligible player and uses `type_launch_attack` to throw that player with enough vertical velocity to reach an approximately 18-block apex. The launch is telegraphed at the target's feet for 20 ticks. Fall damage attributable to that launch is capped at 10 raw damage. Invalid or protected targets cause the cast to end without leaving a bat behind.

## Phases and Pacing

Sahur starts aggressive and becomes less predictable as health falls:

- Phase 1 establishes walk, bat hit, stomp, hunter bat, and charge patterns.
- Phase 2 increases movement pressure and enables stronger use of spin and shield bats.
- Phase 3 shortens attack gaps and summon cooldowns while retaining the same damage values and safety caps.

The coordinator prevents incompatible committed attacks from overlapping. Charge, spin, and launch displacement cannot control the same player simultaneously.

## Damage Ranking and Rewards

Only positive final player damage dealt to Sahur counts. The top three contributors are sorted by total damage descending. Equal totals are resolved by the time at which that total was first reached, so standings are deterministic.

On defeat:

1. First place receives one custom Sahur's Bat.
2. Second place receives three enchanted golden apples.
3. Third place receives three echo shards.

Sahur's Bat is a non-craftable custom catalog item using the supplied `sahurs_bat.json` and exact `sakur.png` texture. It has iron-sword damage (6 attack damage), double iron-sword attack speed (3.2), and iron-sword durability. Its identity and presentation survive item-catalog reload and reconciliation.

Online winners receive rewards directly in their inventory. Inventory overflow drops at that winner's current feet. A winner who disconnects before reward delivery receives a persisted pending reward once on the next join. Reward state is consumed atomically to prevent duplicate claims after reloads or restarts.

The leaderboard is broadcast with placements, player names, damage totals, and prizes. Players who dealt no positive damage are not ranked.

## Assets and Cross-Platform Presentation

The Java boss sources are installed as BetterModel models. Bedrock geometry and animations are installed under the resource-pack entity pipeline and registered through the shared asset registry and Geyser mapping.

The exact supplied `sakur.png` bytes are reused for:

- the Sahur entity;
- every summoned bat type;
- Sahur's Bat reward item.

The supplied Java Sahur model currently lacks the Bedrock export's `sprint` animation. The Java model is repaired by adding a `sprint` animation based on the provided animation data while retaining the rest of the uploaded model. `spin-attack` maps to the supplied Java `spin_in_place` animation. Build verification asserts that every configured animation exists in its target asset.

## Failure Handling

- A charge with no acceptable wall corridor does not start and enters a three-second retry cooldown.
- Player logout, death, teleport, world change, or invalid carrier state releases that player immediately.
- A missing required model, animation, texture, or item definition fails validation at startup or pack generation instead of silently spawning an invisible mechanic.
- Reward delivery records pending state before attempting delivery and removes it only after successful delivery, preventing loss while remaining idempotent.
- Boss removal always clears all transient entities, task handles, model handles, projectile blockers, and passenger relationships.

## Testing and Verification

Test-first coverage includes:

- participant clamping and the 60,000-100,000 health range;
- dynamic wall-ray and player-corridor scoring;
- carrier creation, unique victim pickup, wall release, and cleanup release;
- bat-hit combo probabilities and hard three-hit limit;
- spin per-player hit cooldown and trigger pressure;
- stomp radius and radial knockback policy;
- hunter-bat farthest targeting and bounded bounce route;
- five-bat shield formation and projectile intersection;
- launch selection, height, landing-damage cap, and invalid-target cleanup;
- deterministic top-three ordering and exact rewards;
- durable, exactly-once pending reward delivery;
- Sahur's Bat attack attributes and catalog reconciliation;
- boss selection without breaking the Abyss Guardian;
- Java and Bedrock asset registration, animation presence, and exact texture-byte reuse;
- full transient cleanup on defeat and module shutdown.

Verification runs targeted tests during each red-green-refactor cycle, the full Gradle test suite, resource-pack generation with the BetterModel Java pack workflow, and a Paper server startup check. `gradle clean` is explicitly excluded to preserve the existing runtime state.
