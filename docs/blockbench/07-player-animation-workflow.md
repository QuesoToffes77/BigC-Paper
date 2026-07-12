# Player animation workflow and safety rules

BetterModel supports player limb assets and per-player animation. Treat this as a cosmetic layer with stricter ownership rules than a boss model: a player remains the authoritative Bukkit player at all times.

## 1. Author a limb model

1. Design the player-compatible limb model in Blockbench with the BetterModel player limb layout expected by the installed 3.2.0 release.
2. Save it under the BetterModel player directory, for example:

   ```text
   plugins/BetterModel/players/bigcasares_assassin.bbmodel
   ```

3. Reload BetterModel and validate the asset before exposing it to a command, role, cosmetic, or NPC flow.
4. Obtain player assets through `BetterModel.limb("bigcasares_assassin")`, not `BetterModel.model(...)`. General entity models and player limbs have different lifecycle and profile requirements.

The visual model must have a clear root and only meaningful limbs. More bones mean more display packets, so avoid tiny accessories that do not improve player readability.

## 2. Attach with an explicit owner

Every player animation session needs an owner record containing at least the player UUID, selected limb key, created tracker(s), and restoration state. The owner is the player session or a dedicated cosmetic service—not an ability callback.

Use the player's current profile when creating the limb tracker so skin/profile data remains attached to the correct player. The BetterModel API concept is `BetterModel.limb(...)` plus a player model/profile; keep the exact 3.2.0 adapter call inside one Java gateway rather than duplicating it across listeners.

```text
player joins or opts in
  -> validate permission and current state
  -> resolve BetterModel limb asset
  -> create tracker with that player's profile
  -> retain session ownership
  -> play cosmetic animation
  -> restore and close on every terminal event
```

Never share a player's profile or tracker with another player. Never infer ownership from a display entity UUID alone.

## 3. Non-negotiable gameplay safety

Animations must never own or modify:

- player inventory, held item, armor, or item cooldowns;
- player movement, velocity, flight state, teleport location, or vehicle;
- melee/projectile damage, targeting, invulnerability, hitboxes, or knockback;
- permissions, team membership, scoreboard state, or combat tags.

An ability may ask the cosmetic service to play an animation after it has passed normal gameplay validation, but the animation must not grant the effect or gate its removal. If the model fails to load, the player remains fully playable with vanilla visuals.

## 4. Restore on every terminal event

The owner must restore vanilla player presentation and close BetterModel resources on:

- disconnect and kick;
- death and respawn;
- world change, vehicle transition, spectator transition, or forced disguise removal;
- plugin disable/reload;
- animation cancel, timeout, and any exception during setup.

Make restoration idempotent. A player can die while disconnecting or while the plugin is disabling; repeated cleanup must not duplicate cosmetic state or leave hidden limbs attached.

## 5. Acceptance checklist

- Two nearby players can run different cosmetics without seeing each other's skin/profile applied incorrectly.
- A player disconnects during a looping animation and returns with vanilla movement, inventory, and appearance intact.
- Death and respawn restore first, then allow a new cosmetic session only after the player is ready.
- An animation failure produces a useful server log and leaves combat/movement unchanged.
- A load test uses the expected number of simultaneously animated players and confirms acceptable packet and tick cost.

For shared installation and pack delivery, see [BetterModel setup](00-bettermodel-setup.md). Entity models are covered separately in [boss entity workflow](06-boss-entity-workflow.md).
