# Placeable entity workflow: Nexus

The Nexus is a placeable team asset, not a decorative item. Its gameplay state must survive regardless of whether its Java model can currently be seen.

## Lifecycle

```text
custom item placement
  -> validate player, team, claim, world, and placement location
  -> create and persist Nexus gameplay state
  -> spawn invisible Ghast anchor
  -> attach BetterModel bigcasares_nexus tracker
  -> play state animation
  -> update text/particles/ownership separately
  -> close model before removing anchor
```

The Ghast is the Java server anchor with AI, gravity, and collision disabled. It is invisible but explicitly not Bukkit-invulnerable; Nexus listeners and damage policies govern accepted damage. BetterModel provides the visible bones. Verify its invisibility setting as described in [setup](00-bettermodel-setup.md).

## 1. Model the Nexus

1. Author the BetterModel source as a Generic Model and keep a Bedrock Entity export copy.
2. Make `root` the only top-level bone. Put base, core, frame, crystals, and effects beneath it.
3. Place the base at the ground contact so the anchor location represents the placed block position. Do not compensate for a wrongly placed root with arbitrary Java offsets.
4. Keep the model compact enough for its actual interaction radius. Decorative collision must never substitute for the Nexus's server-side protection rules.
5. Add exactly these animation names:

   - `idle`: normal looping state.
   - `damaged`: one-shot feedback after valid damage.
   - `critical`: looping state at or below 25% health.
   - `destroyed`: one-shot visual teardown.

6. Deploy the Java source to `plugins/BetterModel/models/bigcasares_nexus.bbmodel` and export the Bedrock geometry/animations separately. The repository does not currently package this Nexus `.bbmodel`.

## 2. Map gameplay state to animation state

| Gameplay event | Java request | Bedrock state |
| --- | --- | --- |
| Spawn or recovered healthy Nexus | `idle` | normal/secure |
| Damage accepted | steady state update, then `damaged` | feedback requires separate state synchronization |
| Health at or below 25% | `critical` | critical |
| Destroyed | `destroyed` request, then immediate close on removal | clip exists; controller transition is not wired |

`BAJO ATAQUE` is a team/attacker state, not automatically a health threshold. Preserve its existing attacker tracker and text/particle behavior; use it to drive visual feedback only where the design calls for it. `critical` is the low-health animation.

The current Java gateway has no callback to restore the steady loop after `damaged` or to await `destroyed`. Bedrock's shared controller only selects idle/critical from entity health queries; gameplay health is maintained separately. Treat loop restoration and Bedrock health/feedback parity as client acceptance checks, not implemented guarantees.

## 3. Restart recovery

Persist Nexus identity, owner team, world, location, and gameplay health. On startup:

1. Load persisted Nexus records.
2. Locate a valid existing anchor by its persistent Nexus ID, or create one if none exists.
3. Attach exactly one tracker to that anchor.
4. Select `idle` or `critical` from persisted health.
5. Recreate health text and safe particle tasks without duplicating them.

Never persist a BetterModel tracker, display entity UUID, or animation callback. They are runtime resources and must be rebuilt from the persisted Nexus record.

## Manual acceptance checklist

- Place a Nexus as a team member and confirm it anchors at the intended block position.
- Attempt placement without a team, in an invalid location, and where a team already has a Nexus; confirm no model or anchor leaks.
- Damage it once: inspect `damaged` playback and subsequent steady-loop behavior; report any missing restoration.
- Reduce it below 25%: `critical` loops and text/ownership remains correct.
- Move an attacker inside and outside the configured range: the `BAJO ATAQUE` state changes without corrupting health state.
- Destroy it: the teardown request is issued and its tracker closes before anchor removal; full `destroyed` playback is not currently awaited.
- Restart while it is healthy and while it is critical: exactly one model, one text display, and one anchor return.
- Join using Geyser and confirm native Bedrock Nexus geometry and controller behavior still work.

See [BetterModel setup](00-bettermodel-setup.md) for deployment and [Java animations](04-java-animations-and-registration.md) for the pack merge.
