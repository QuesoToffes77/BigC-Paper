# Phase 0 Reload Lifecycle Handoff

**Project:** BigCasares
**Repository:** `bigcasares-paper`
**Branch:** `main`
**Date:** 2026-07-15
**Status:** Implementation in progress; not ready to merge or deploy

## Original objective

Investigate the local Oraxen source and compare its resource-pack, custom-item,
sound, entity, and reload architecture with BigCasares. The resulting investigation
split the work into phases. This workspace currently implements only Phase 0:
safe runtime ownership and reload lifecycle foundations.

The investigation is in
`docs/superpowers/specs/2026-07-15-oraxen-inspired-content-runtime-investigation.md`.
The approved Phase 0 design and execution plan are in:

- `docs/superpowers/specs/2026-07-15-reload-lifecycle-safety-design.md`
- `docs/superpowers/plans/2026-07-15-reload-lifecycle-safety.md`

## What has been implemented

### Runtime ownership core

- Added a generation identity used to retire callbacks from an old runtime.
- Added per-module registration scopes with idempotent, reverse-order cleanup.
- Added named cleanup outcomes/reports that continue cleaning after one action fails.
- Added Bukkit adapters for listeners, tasks, recipes, command bindings, closeables,
  guarded scheduling, and a temporary plugin-wide listener/task fallback.
- Added source-compatible scoped module activation through `PluginModule`.
- Changed `ModuleManager` to allocate one scope per enabled module, tear down partial
  activation, disable modules in reverse order, and return structured lifecycle reports.

Primary packages:

- `src/main/java/dev/linqfy/bigCasares/module/runtime/`
- `src/main/java/dev/linqfy/bigCasares/module/`

### Structured reload orchestration

- Added single-flight reload coordination.
- Added generation IDs, reload phases, statuses, duration, warnings, lifecycle
  reports, cleanup reports, and failure details.
- Added command-facing Spanish result formatting.
- Routed `BigCasaresCommand` through the structured result instead of always printing
  `Configs recargadas.`.
- Began integrating initial activation, shutdown, reload, temporary fallback cleanup,
  and runtime-reference clearing in `BigCasares`.

Primary package: `src/main/java/dev/linqfy/bigCasares/reload/`.

### Gameplay module ownership migration

Scoped and guarded the current runtime resources for:

- Copper Apple
- Smoke Bomb
- Custom Crossbow
- Missions and mission HUD
- Skill Rating
- Inventory Limit
- Airdrop
- Bounty lifecycle wiring

This includes listeners, recipes, custom-item registrations, delayed/repeating tasks,
command bindings, landing callbacks, and open mission HUD views. Airdrop task leases
are detached after normal completion so later scope cleanup does not retain completed
tasks.

The Bounty files already contained unrelated in-progress payment-voucher, balance,
withdrawal, command, and storage work. Those changes belong to the user and were
preserved; only lifecycle wiring was added around the current implementation.

### Infrastructure and presentation module ownership migration

Scoped or guarded the current resources for:

- Resource Pack status listener and delayed request/timeout work
- Teams listener and PlaceholderAPI expansion
- Nexus listeners, tasks, item, gateway, models, entities, and presentations
- Shop listeners, NPCs, GUI callbacks, and presentations
- PvE outer runtime ownership
- Geyser subscription and queued Bukkit/form callbacks
- Moderation listener, tasks, log handler, and alerts
- Server Control listener, tasks, command bindings, sessions, and queued callbacks
- Discord task, audit sink, bridge callbacks, JDA gateway, HTTP/coalescer callbacks

The existing dirty Nexus placement-policy/rejection/test changes were not part of
this migration and must not be overwritten.

### PvE transient-resource ownership

- Added explicit tracking for transient projectile `ItemDisplay` entities and cloned
  Wardens.
- Tracks/cancels associated main and ability tasks.
- Removes tracked transient entities during shutdown.
- Guards delayed combat/removal callbacks against retired generations.

Primary files:

- `PaperAbyssGuardianRuntime.java`
- `PveTransientOwner.java`
- `PveTransientCleanupFailure.java`

## Tests added

- `ModuleManagerLifecycleTest`
- `RuntimeRegistrationScopeTest`
- `BukkitRuntimeRegistrationsTest`
- `ReloadCoordinatorTest`
- `ReloadResultTest`
- `ReloadMessageFormatterTest`
- `PveTransientOwnerTest`

## Verification already performed

- Runtime/reload core agent: 40 focused tests passed.
- Gameplay migration agent: 114 focused tests passed.
- PvE transient migration agent: 31 focused PvE tests passed.
- Root `compileJava --offline`: passed before the final infrastructure migration.
- Infrastructure migration: all 12 directly touched files passed a manual JDK 25
  compilation using cached project dependencies.
- Infrastructure migration `git diff --check`: passed.

These results are partial and overlap. A complete Gradle test/build has not been run
against the combined final workspace. Do not claim Phase 0 complete from these numbers.

## Blocking review findings still to fix

1. **Generation retirement race.** `RuntimeGeneration.guard()` checks active state and
   then invokes the callback separately. Retirement can occur between those operations.
   Guarded execution and `retire()` must be mutually ordered, with a deterministic
   latch-based concurrency test proving no guarded mutation can continue after
   `retire()` returns.

2. **Partial runtime references can survive failure.** `BigCasares.disableCurrentRuntime()`
   returns early when the manager is null and does not clear references in a `finally`.
   Candidate construction or module-disable exceptions can therefore leave partial or
   stale fields published.

3. **Failed candidate generation IDs are reused.** `ReloadCoordinator` derives a
   candidate from the last successful generation and advances only on success. Use a
   separate monotonic reservation sequence so every attempted runtime has a unique ID.

4. **The generation contract is optional.** `ReloadOperation` currently permits an
   implementation to ignore the coordinator-supplied generation. Make the
   generation-bearing initialization method mandatory and make the ten-cycle test use
   the supplied generation object.

5. **Some failure messages are false.** A preparation failure happens before shutdown,
   so the previous runtime remains active. An internal failure can leave uncertain
   state. `ReloadMessageFormatter` must not say the runtime was stopped in those cases.

6. **Required command bindings are not validated.** `BigCasares.registerCommands()`
   silently skips missing `PluginCommand` entries, making a command-binding reload
   failure unreachable. Resolve and require all expected commands before rebinding,
   while preserving the `/bigcasares` command as the retry path.

## Important non-blocking hardening

- Reject module activation with an already-retired generation.
- Restore both the prior completer and executor even if one command restoration throws.
- Preserve cleanup resource IDs when translating cleanup failures into lifecycle
  outcomes, or document that diagnostic limitation.
- Decide whether to enforce Bukkit primary-thread execution at the public reload entry.
- Consider retaining the old two-argument `JavaNexusVisualGateway` constructor for
  source compatibility; repository call sites currently use the new scoped constructor.
- Geyser queued callbacks are generation-guarded, but in-flight lifecycle-definition
  handlers currently rely on event-bus unregister/close.

## Ordered next steps

1. Re-read `GUIDELINES.MD`, the investigation, Phase 0 design, this handoff, and the
   current dirty worktree before editing.
2. Fix each of the six blocking review findings test-first. Run the focused test after
   each correction.
3. Apply the non-blocking hardening where it is low-risk, especially retired-generation
   rejection and failure-tolerant command restoration.
4. Review the combined module migrations for conflicts or duplicate cleanup. Pay special
   attention to Bounty, Nexus, PvE, Discord/JDA, Geyser, and command restoration.
5. Remove the untracked `.gradle-user-home-task5/wrapper/` directory created by a failed
   sandbox Gradle bootstrap. Confirm it contains no user work before removal.
6. Run all focused lifecycle, reload, command, PvE, and modified-module tests.
7. Run the entire test suite with a writable Gradle home.
8. Run `clean build`, then `git diff --check` and inspect `git status --short` for
   generated or unrelated changes.
9. Confirm the deterministic ten-cycle reload test leaves exactly one active registration
   set and all callbacks from retired generations are inert.
10. Update the Phase 0 plan checkboxes and completion report using only the actual final
    results. Do not mark work complete if any combined-suite or build failure remains.

Suggested verification commands:

```bash
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew test \
  --tests 'dev.linqfy.bigCasares.module.*' \
  --tests 'dev.linqfy.bigCasares.reload.*' \
  --tests 'dev.linqfy.bigCasares.command.BigCasaresCommandTest' \
  --tests 'dev.linqfy.bigCasares.modules.pveboss.*'

GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew test
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew clean build
git diff --check
git status --short
```

## Explicitly not implemented yet

Phase 0 does **not** implement Oraxen-style runtime content or resource-pack hot reload.
The following remain future, separately approved phases:

- YAML/content discovery and validation
- immutable custom-item/sound/entity content snapshots
- deterministic resource-pack generation and merge rules
- staging and atomic promotion
- content hashing and no-op rebuild detection
- pack hosting/upload/versioned URLs
- client pack resend and status reconciliation
- inventory/entity reconciliation across schema changes
- BetterModel and Geyser definition regeneration
- transactional prepare/commit/rollback

The next feature phase should start only after Phase 0 passes the full verification gate.
Its first deliverable should be an offline deterministic pack build pipeline, not live
server publication.

## Temporary fallback removal condition

Keep the plugin-wide Bukkit listener/task cleanup fallback until integration coverage
proves that every plugin-owned listener, task, command binding, subscription, closeable,
presentation, and transient entity has explicit ownership, and ten live reload cycles on
a Paper server leave no fallback-discovered registrations. Remove it only after those
conditions are documented and repeatable.
