# Jeremy Module Design

## Objective

Add one global vanilla Zombie hunter that rests for one real hour, selects one eligible player, hunts for seven real minutes, and returns to rest after timeout, logout, target death, Jeremy death, or administrative stop.

## Architecture

- `JeremyModule` owns one listener, one repeating runtime task, one command binding, and one cleanup action through `RuntimeRegistrationScope`.
- `JeremyCycle` owns the `RESTING`, `HUNTING`, and `CELEBRATING` state transitions using epoch-millisecond deadlines.
- `YamlJeremyStorage` persists phase, target, previous target, and the current phase deadline atomically.
- `JeremyTargetSelector` filters candidates and avoids the previous target when alternatives exist.
- `JeremySpawner` finds loaded, solid, non-liquid spawn locations 24-40 blocks from the target.
- `JeremyRuntime` orchestrates the current UUID-based session and vanilla Zombie navigation.

## Identity And Targeting

Jeremy is an adult vanilla Zombie marked with `bigcasares:custom_entity=jeremy` and a target UUID in PDC. Display name is presentation only. Survival and Adventure players in enabled worlds are eligible by default; Creative, Spectator, dead, offline, invalid, NPC, and excluded-world players are rejected.

The selected target remains fixed. `EntityTargetLivingEntityEvent` restores the session target after retaliation or vanilla target changes. Same-world extreme distance triggers a delayed safe reposition. World changes respawn Jeremy near an eligible target without resetting the seven-minute deadline.

## Power And Combat

Power is calculated at hunt start and every 200 ticks from armor, toughness, Protection levels, max health, absorption, Resistance, held weapon, and offensive enchantments. It maps to configurable melee values from 4 to 11 HP. Health starts at 40 and scales gradually to a maximum of 1.25x.

An equipped `GOLDEN_HELMET` multiplies direct player damage against Jeremy by 1.5. Inventory items, gold armor in other slots, other helmets, and other victims are unchanged.

Jeremy uses the measured vanilla baby-zombie equivalent movement speed: adult base `0.23` plus the vanilla 50% baby modifier equals `0.345`.

## Pathfinding And Ultrasound

Normal movement uses Zombie pathfinding. A sampled progress detector compares both Jeremy displacement and distance closed toward the target. Four seconds without at least 1.5 blocks of progress marks the session stuck.

Ultrasound then charges for 30 ticks with a visible and audible warning. It uses an eye-to-eye ray independent of interaction range, has a 32-block range, deals 0.8x current melee damage, applies 0.4 knockback, and enters a 100-tick cooldown. Manual ray traversal permits at most two unique solid blocks, so thin barriers can be penetrated but mountains cannot.

## Celebration And Cleanup

A target death within five seconds of Jeremy damage starts an 80-tick celebration. Navigation and attacks stop while Jeremy rotates and emits restrained vanilla feedback. Other deaths end the hunt without celebration.

Timeout, logout, death, excluded world change, world unload, missing entity, stop, and plugin disable remove the owned entity and references. Plugin disable preserves an active real-time deadline so reload/restart can resume safely; stale PDC entities are removed before recovery.

## Performance And Assets

There is one two-tick scheduler for the module. Resting selection runs once per second, power once per ten seconds, and stuck sampling once per second. No entity-wide scans occur per tick and no chunks are forced or kept loaded.

Jeremy uses only vanilla particles, sounds, and Zombie rendering. No resource-pack model, texture, ItemModel, CustomModelData, or BetterModel asset is created.
