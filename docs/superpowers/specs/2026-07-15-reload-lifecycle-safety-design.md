# Reload Lifecycle Safety Design

**Date:** 2026-07-15
**Phase:** 0 only
**Source:** `2026-07-15-oraxen-inspired-content-runtime-investigation.md`

## Objective

Make the existing `/bigcasares reload` operation repeatable, observable, and honest before BigCasares gains runtime content or resource-pack generation. Every enabled module receives an explicit `RuntimeRegistrationScope` tied to the current runtime generation. The scope owns listeners, tasks, recipes, command bindings, external subscriptions, closeables, and transient presentation handles; retiring the generation prevents stale callbacks, and closing its module scopes releases resources once in reverse order.

This is a clean-room BigCasares design. It adopts the operational goal of reliable hot reload without copying Oraxen code, configuration, assets, names, or mutable-global architecture.

## Scope

### In scope

- Inventory every current listener, scheduled task, recipe, command binding, external subscription, closeable gateway, custom-item registration, and transient presentation handle.
- Create one runtime generation for each plugin-state activation and one explicit registration scope for every enabled module in that generation.
- Close module scopes in reverse module-enable order and close resources inside each scope in reverse acquisition order.
- Make scope close, module disable, candidate cleanup, and plugin shutdown idempotent.
- Guard delayed and external callbacks so a retired generation cannot mutate a replacement runtime.
- Migrate all current modules to explicit ownership, including anonymous listeners and resources that are currently only partially cleaned up.
- Keep a plugin-wide Bukkit listener/task cleanup as a temporary migration safety net after explicit scope closure.
- Make `ModuleManager` return ordered lifecycle outcomes and treat any enable failure as a failed activation.
- Immediately clean a module whose `onEnable` fails and tear down every earlier module in the failed generation.
- Add single-flight reload orchestration and structured `SUCCESS`, `NO_OP`, `ALREADY_RUNNING`, and `FAILED` results.
- Make `BigCasaresCommand` render the actual reload result instead of unconditional success.
- Prove ten repeated reload cycles leave exactly one active registration set and only the newest generation may run callbacks.

### Out of scope

- Runtime pack discovery, generation, staging, validation, publishing, hosting, upload, dispatch, hashing, or pack identity changes.
- A runtime `plugins/BigCasares/content` directory or extraction of seed assets.
- Declarative item, sound, recipe, or entity-presentation definitions.
- Item or entity reconciliation and item PDC identity migration.
- BetterModel reload commands, model-definition reconciliation, or changes to BetterModel pack output.
- Geyser resource-pack, custom-item, or custom-entity redefinition.
- Transactional rollback to the previous configuration/runtime after the old generation is retired.
- Generic furniture, blocks, mechanics, glyphs, shaders, armor, obfuscation, or multi-version packs.
- Dependency, Java, Minecraft, Spigot/Paper, BetterModel, Geyser, or resource-pack asset changes.

## Confirmed Rules

- Java remains 21 and the Minecraft/Spigot/Paper baseline remains 1.21.11.
- Only one plugin-state reload may execute at a time. A competing request returns `ALREADY_RUNNING` and performs no lifecycle work.
- Each activation owns a distinct `RuntimeGeneration`; each enabled module owns a distinct `RuntimeRegistrationScope` tied to that generation.
- A generation is retired before its modules begin shutdown. Guarded callbacks check the generation immediately before domain mutation and become no-ops once it is retired.
- Active modules shut down in reverse enable order. Inside each module scope, cleanup actions run once in reverse acquisition order.
- One cleanup failure is recorded and logged but does not stop later cleanup actions.
- Closing a scope or disabling a module more than once is harmless and never repeats destructive cleanup.
- `ModuleManager` creates the scope before enabling a module. If enable throws after acquiring resources, the manager invokes the module's idempotent shutdown and closes that scope immediately.
- A failed module activation stops candidate activation. All earlier modules in that generation are disabled and closed in reverse order; no partial generation remains active.
- Plugin-wide `HandlerList.unregisterAll(plugin)` and scheduler `cancelTasks(plugin)` run only as a temporary fallback after explicit module-scope closure. Owned transient entities and their removal actions must close before scheduler cancellation; otherwise cancellation can strand those entities. The fallback is not the ownership model and does not satisfy the per-module tests.
- Listeners, tasks, recipes, command executors/tab completers, event-bus subscriptions, closeables, custom-item registrations, and transient presentations acquired by a module must have a matching scope action.
- Reload failures are returned as structured data as well as logged. A green success message is forbidden when disable, cleanup, config reload, activation, or command binding fails.
- Phase 0 fails closed after the old generation has been retired. It does not claim or attempt transactional rollback; that belongs to the later content-runtime phases.
- `NO_OP` is a supported orchestration and presentation result. The current full config reload normally produces `SUCCESS`; Phase 0 does not add config hashing merely to manufacture no-op detection.
- Result data is immutable and contains status, duration, old/new generation IDs when known, failure phase when applicable, ordered module outcomes, cleanup failures, and safe warnings.
- Player-facing messages remain Spanish and use the project's current color style. Detailed exceptions remain in server logs.
- The existing `bigcasares.shop.reload` permission remains the compatibility permission in Phase 0. Subsystem permissions arrive with scoped reload commands later.
- Pack building and delivery behave exactly as before this phase.

## Current Ownership Inventory

| Owner | Runtime resources Phase 0 must own |
|---|---|
| Copper Apple | custom-item registration, recipe key, craft listener |
| Smoke Bomb | custom-item registration, recipe key, craft/projectile/visibility listeners, projectile heartbeat and concealment cleanup |
| Custom Crossbow | custom-item registration, recipe key, seven listeners, projectile heartbeat, charge loads, delayed sonic-trail callbacks |
| Missions | mission listener, HUD listener/controller and any HUD-delayed work |
| Bounties | bounty and payment-paper listeners; `bounty`, `bal`, and `withdraw` executor/completer bindings |
| Inventory Limit | listener and delayed enforcement callbacks |
| Skill Rating | listener |
| Airdrop | listener, interval and falling tasks, `airdrop` command binding |
| Resource Pack | status listener and delayed retry/kick callbacks |
| Teams | listener and PlaceholderAPI expansion registration |
| Nexus | two retained listeners, anonymous placement listener, restore/attack/aura tasks, custom-item registration, model/presentation/entity handles |
| Shop | GUI/NPC listeners, spawned NPCs and client-presentation registrations |
| PvE Boss | Paper runtime listener, scheduler and attack/removal tasks, projectile `ItemDisplay` entities, cloned Warden entities, bosses, models, music/presentation handles |
| Geyser | external event-bus subscription and main-thread bridge callbacks |
| Moderation | listener, asynchronous purge task, rare-item task, log handler and queued alerts |
| Server Control | listener, maintenance task, queued main-thread work and resistance sessions |
| Discord | reconciliation task, audit sink, server-control callbacks, JDA gateway/coalescer and JDA-to-Bukkit callbacks |
| Plugin command bootstrap | root command executor/completer replacements that are refreshed by plugin-state activation |

The implementation begins by verifying this inventory against the live source. Newly discovered registrations join the owning row; they are not deferred merely because they were absent from this table.

## Architecture

### `RuntimeGeneration`

An immutable process-local generation ID plus a thread-safe active/retired state. It exposes guarded `Runnable`, supplier, or consumer boundaries used immediately before callbacks enter mutable Bukkit/domain state. Retirement is idempotent. Generation IDs are diagnostic and are not persisted.

### `RuntimeRegistrationScope`

An idempotent reverse-order collection of named cleanup actions bound to one `RuntimeGeneration` and one owner ID. The pure core accepts teardown actions and closeables without Bukkit dependencies, aggregates every cleanup failure, rejects new ownership after close, and exposes guarded callback wrappers.

A Bukkit adapter registers and owns listeners, scheduler tasks, recipes, command executor/completer replacements, and plugin-wide fallback cleanup. Modules use the adapter rather than registering anonymous resources with no teardown. External subscriptions, custom-item registrations, models, entities, and other gateways use named core cleanup actions.

The runtime generation owns the ordered module scopes. The generation retires once, then `ModuleManager` disables modules and closes their scopes in reverse order. A scope is never shared between modules.

### `PluginModule` and `ModuleManager`

`PluginModule` gains a source-compatible enable entry point that accepts its scope, or an equivalent narrowly scoped context. `ModuleManager` always calls the scoped entry point, records the module and its scope together, and owns their reverse teardown. All production modules are migrated during Phase 0; the compatibility entry point exists only to avoid breaking test fakes and is not permission to leave production registrations unowned.

Per-module outcomes distinguish disabled-by-config, enabled, enable-failed, disabled, disable-failed, and cleanup-failed. Enable failure triggers immediate idempotent module shutdown plus scope close. The generation activation stops on the first enable failure and reverses previously active modules. Disable and cleanup continue after individual failures so the report is complete.

### `ReloadCoordinator`

A pure single-flight coordinator wraps the existing synchronous plugin-state operation. It owns the compare-and-set in-progress guard, timestamps, generation transition metadata, result construction, and `finally` release of the reload slot. A small operation interface lets tests simulate success, no-op, lifecycle failure, stale callbacks, and a concurrent request without starting Bukkit.

The Bukkit integration remains on the primary server thread. The real operation retires the current generation, disables and closes its modules, verifies owned transient presentations/entities were removed, runs the temporary fallback cleanup, reloads configuration, constructs a fresh generation, and enables its modules. Failure after retirement leaves the plugin fail-closed; Phase 0 does not restore the old config/runtime.

### Structured Results and Command Presentation

`ReloadResult` contains `ReloadStatus` (`SUCCESS`, `NO_OP`, `ALREADY_RUNNING`, `FAILED`), duration, generation IDs, optional failure phase, ordered lifecycle outcomes, cleanup failures, and warnings. `ReloadMessageFormatter` is pure and maps every status to a concise Spanish message.

`BigCasares#reloadPluginState()` returns the result. `BigCasaresCommand` sends the formatter output exactly once. Success, no-op, already-running, and failure have distinct messages; server logs retain technical diagnostics.

### Stale Callback Prevention

Task cancellation alone is insufficient because one-shot Bukkit work, already-queued main-thread work, asynchronous work, and external JDA/Geyser callbacks may already be in flight. Every such callback captures the owning generation guard and checks it immediately before state mutation. Cancellation/close remains required; the guard is the last boundary, not a substitute for cleanup.

## Config Shape

Phase 0 adds no configuration files or keys. Existing `modules.<module-id>.enabled` values, module settings, command declarations, and the transitional `bigcasares.shop.reload` permission retain their current meaning.

## Boundaries

- Do not modify `resourcepack/`, generated pack tasks, bundled manifests, hashes, URLs, delivery modes, or player pack-state semantics.
- Do not create content snapshots, runtime content folders, staging directories, active manifests, publishers, or an HTTP server.
- Do not change item identity, model data, sounds, gameplay rules, persisted data formats, module IDs, or enable defaults.
- Do not claim BetterModel or Geyser content was dynamically redefined.
- Do not introduce a mutable static ownership registry or global singleton.
- Do not use Mockito or another mocking framework.
- Do not hide lifecycle failures or expose raw exception details to players.
- Do not treat the plugin-wide Bukkit cleanup as the final ownership architecture. It is a temporary Phase 0 safety net with an explicit removal condition in the completion report.
- Preserve all unrelated working-tree changes, especially the in-progress Bounty and Nexus work. Bounty lifecycle edits must be minimal, re-read the current file immediately before patching, and must not overwrite its payment-voucher or command additions.
- Do not begin Phase 1 or any later migration in this change.
