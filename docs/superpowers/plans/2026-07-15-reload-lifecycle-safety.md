# Reload Lifecycle Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Implement Phase 0 from the Oraxen-inspired investigation: give every enabled BigCasares module its own generation-bound registration scope, retire and close all runtime resources safely in reverse order, prevent callbacks from retired generations, make ten repeated full reloads leave one active registration set, reject overlapping reloads, and report structured success/no-op/already-running/failure results. Resource-pack, item, sound, and entity-content hot reload remain unchanged and out of scope.

## Tech Stack

- Java 21
- Minecraft 1.21.11
- Spigot API `1.21.11-R0.1-SNAPSHOT` / Paper-compatible server architecture
- Gradle with Groovy DSL
- JUnit 5.11.4
- No new dependencies and no dependency-version changes

## File Structure

### New production files

- `src/main/java/dev/linqfy/bigCasares/module/runtime/RuntimeGeneration.java` — process-local identity and active/retired callback guard.
- `src/main/java/dev/linqfy/bigCasares/module/runtime/RuntimeRegistrationScope.java` — per-module idempotent reverse-order cleanup core.
- `src/main/java/dev/linqfy/bigCasares/module/runtime/RuntimeCleanupFailure.java` — named immutable cleanup failure.
- `src/main/java/dev/linqfy/bigCasares/module/runtime/RuntimeCleanupResult.java` — aggregate cleanup result.
- `src/main/java/dev/linqfy/bigCasares/module/runtime/BukkitRuntimeRegistrations.java` — Bukkit listener/task/recipe/command adapters around a module scope.
- `src/main/java/dev/linqfy/bigCasares/module/ModuleLifecycleStatus.java` — module lifecycle states.
- `src/main/java/dev/linqfy/bigCasares/module/ModuleLifecycleOutcome.java` — immutable per-module result.
- `src/main/java/dev/linqfy/bigCasares/module/ModuleLifecycleResult.java` — ordered aggregate result.
- `src/main/java/dev/linqfy/bigCasares/reload/ReloadStatus.java` — success, no-op, already-running, failed.
- `src/main/java/dev/linqfy/bigCasares/reload/ReloadPhase.java` — safe phase identifiers.
- `src/main/java/dev/linqfy/bigCasares/reload/ReloadResult.java` — immutable command-facing reload outcome.
- `src/main/java/dev/linqfy/bigCasares/reload/ReloadOperation.java` — pure orchestration boundary.
- `src/main/java/dev/linqfy/bigCasares/reload/ReloadCoordinator.java` — synchronous single-flight coordinator.
- `src/main/java/dev/linqfy/bigCasares/reload/ReloadMessageFormatter.java` — pure Spanish result formatter.

Exact grouping may be reduced when records/enums are cohesive. Keep Bukkit dependencies out of the runtime core, coordinator, result, and formatter tests.

### Modified production files

- `src/main/java/dev/linqfy/bigCasares/BigCasares.java` — own generation lifecycle, temporary fallback cleanup, structured reload, and truthful command refresh outcome.
- `src/main/java/dev/linqfy/bigCasares/module/PluginModule.java` — add the narrowly scoped/source-compatible ownership entry point.
- `src/main/java/dev/linqfy/bigCasares/module/ModuleManager.java` — create one scope per enabled module and aggregate enable/disable/cleanup outcomes.
- `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java` — render `ReloadResult` instead of unconditional success.
- `src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleModule.java` — own its item, recipe, and craft listener.
- `src/main/java/dev/linqfy/bigCasares/modules/smokebomb/SmokeBombModule.java` and `SmokeBombProjectileListener.java` — own listeners, recipe, item, heartbeat, and concealment shutdown.
- `src/main/java/dev/linqfy/bigCasares/modules/customcrossbow/CustomCrossbowModule.java`, `CustomCrossbowChargeListener.java`, `CustomCrossbowShootListener.java`, and `PrismarineArrowListener.java` — own seven listeners and all heartbeat/delayed work.
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionModule.java` and `MissionHudController.java` — own mission/HUD registrations and delayed work.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyModule.java` — minimally own the current dirty module's listeners and command bindings.
- `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitModule.java` — own its listener and guard delayed enforcement.
- `src/main/java/dev/linqfy/bigCasares/modules/skillrating/SkillRatingModule.java` — own its listener.
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropModule.java` — own its listener, tasks, landing callback, and command binding.
- `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackModule.java` and `ResourcePackStatusListener.java` — own the status listener and guard delayed responses.
- `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamModule.java` — own its listener and PlaceholderAPI expansion.
- `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModule.java` and `JavaNexusVisualGateway.java` — own all three listeners, tasks, item, models, entities, and presentations without changing placement policy.
- `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java` and `ShopNpcFactory.java` — own GUI/NPC listeners, entities, and presentation entries.
- `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java` and `PaperAbyssGuardianRuntime.java` — own listeners, tasks, projectile displays, cloned Wardens, bosses, models, music, and presentations.
- `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserIntegrationModule.java` and `GeyserRuntimeBridge.java` — own the external subscription and guard queued Bukkit callbacks.
- `src/main/java/dev/linqfy/bigCasares/modules/moderation/ModerationModule.java` — own listener, scheduled work, queued alerts, and log handler.
- `src/main/java/dev/linqfy/bigCasares/modules/servercontrol/ServerControlModule.java` — own listener, maintenance/queued work, and resistance sessions.
- `src/main/java/dev/linqfy/bigCasares/modules/discord/DiscordIntegrationModule.java` and `JdaDiscordGateway.java` — own task, audit sink, bridge callbacks, JDA/coalescer, and guarded external callbacks.

Only modify a directly owned runtime/listener file when its current API cannot express explicit cleanup or generation guarding. Do not rewrite domain behavior for consistency.

### New and modified tests

- `src/test/java/dev/linqfy/bigCasares/module/runtime/RuntimeRegistrationScopeTest.java` — reverse close, all-failure collection, idempotency, close-state registration rejection, and stale guard.
- `src/test/java/dev/linqfy/bigCasares/module/ModuleManagerLifecycleTest.java` — one scope per module, failed-enable cleanup, reverse disable, structured outcomes, and repeated disable.
- `src/test/java/dev/linqfy/bigCasares/reload/ReloadCoordinatorTest.java` — ten cycles, single flight, status/failure phase, generation transitions, and guard reset.
- `src/test/java/dev/linqfy/bigCasares/reload/ReloadMessageFormatterTest.java` — Spanish output for every terminal status.
- `src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java` — structured reload presentation boundary and existing routing.
- Focused module lifecycle tests only where hand-written fakes can verify resource ownership without a live Bukkit server.

## Boundaries

- Phase 0 only. Do not add runtime content discovery, pack generation, staging, publishing, upload, hosting, dispatch, versioned URLs, content hashes, or pack resending.
- Do not modify `resourcepack/`, pack Gradle tasks, bundled artifacts, BetterModel pack behavior, or Geyser definitions.
- Do not add item/sound/entity YAML, change custom-item identity, or reconcile inventories/entities.
- Do not change Java 21, Minecraft/Spigot/Paper 1.21.11, dependencies, module IDs, config defaults, persisted formats, permissions, or gameplay rules.
- Preserve unrelated working-tree work. In particular, make only lifecycle-focused edits to the dirty Bounty module after re-reading its latest contents and diff; do not overwrite payment-voucher/command changes. Avoid unrelated Nexus edits.
- Use test-first development, immutable values, hand-written fakes, no Mockito, no wildcard imports, and no unnecessary comments.

## Tasks

1. Confirm baseline and finish the ownership inventory
   - Files affected: no production files; update this plan only with discoveries while executing.
   - [x] Run focused existing module-manager, command, resource-pack, Geyser, model, Nexus, boss, and affected-module tests before editing.
   - [x] Search every production class for listener registration, scheduler use, recipes, executor/completer replacement, event-bus subscription, closeables, custom-item registration, and transient entity/model ownership.
   - [x] Map every registration to the module row in the companion design and classify current teardown as complete, incomplete, or stale-callback-prone.
   - [x] Re-read `git status --short` and the current Bounty/Nexus diffs before any edit touching those files.
   - [x] Record unrelated baseline failures separately.
   - Verification: every registration has one named owner and planned teardown; baseline results and dirty files are recorded.

2. Build the generation and per-module scope core test-first
   - Files affected: `module/runtime/RuntimeGeneration.java`, `RuntimeRegistrationScope.java`, cleanup result types, and `RuntimeRegistrationScopeTest.java`.
   - [x] Write failing tests proving resources close once in reverse acquisition order.
   - [x] Write a failing test proving all later cleanups run and named failures aggregate when one cleanup throws.
   - [x] Write a failing test proving close is idempotent and registration after close is rejected.
   - [x] Write a failing test proving a guarded callback cannot mutate state after generation retirement.
   - [x] Implement a thread-safe generation and pure per-owner cleanup scope without Bukkit dependencies or mutable public statics.
   - [x] Retire the generation before any associated resource cleanup begins.
   - Verification: core scope tests pass without Bukkit, sleeps, network, or mocks.

3. Add Bukkit ownership adapters test-first
   - Files affected: `BukkitRuntimeRegistrations.java` and focused adapter tests/fakes.
   - [x] Define adapters for listener registration/unregistration, owned `BukkitTask` cancellation, recipe removal, and command executor/completer restoration.
   - [x] Ensure cleanup actions are added only after successful acquisition and carry readable owner/resource names.
   - [x] Provide guarded scheduling entry points for immediate, delayed, repeating, and externally queued callbacks.
   - [x] Keep plugin-wide listener/task cleanup in a separate temporary fallback method so modules cannot confuse it with explicit ownership.
   - Verification: adapter behavior is covered through small interfaces/fakes where Bukkit static state would otherwise require a server.

4. Make module lifecycle activation structured and scope-aware
   - Files affected: lifecycle result types, `PluginModule.java`, `ModuleManager.java`, and `ModuleManagerLifecycleTest.java`.
   - [x] Write failing fake-module tests for disabled-by-config, enabled, enable-failed, disabled, disable-failed, and cleanup-failed outcomes.
   - [x] Write a failing test proving every enabled module receives a distinct scope tied to the same current generation.
   - [x] Write a failing test proving a partial enable invokes idempotent module shutdown and closes its acquired resources immediately.
   - [x] Write a failing test proving an enable failure stops activation and tears down earlier modules/scopes in reverse order.
   - [x] Write a failing test proving disable continues after failure and repeated disable does not clean anything twice.
   - [x] Preserve config compatibility and duplicate-ID behavior while returning immutable ordered reports and retaining logger stack traces.
   - Verification: lifecycle tests and existing `ModuleManagerConfigurationTest` pass using hand-written fakes.

5. Migrate item/recipe and gameplay-listener modules to explicit scopes
   - Files affected: Copper Apple, Smoke Bomb, Custom Crossbow, Missions, Skill Rating, Inventory Limit, and their directly owned runtime/listener classes.
   - [x] Own custom-item registration, recipe removal, and listener teardown for Copper Apple.
   - [x] Own all Smoke Bomb listeners, heartbeat/concealment shutdown, recipe, and custom-item registration.
   - [x] Own all Custom Crossbow listeners, heartbeat/charge tasks, delayed load/trail callbacks, recipe, and custom-item registration.
   - [x] Own Mission and HUD listeners/work and Skill Rating listener; clear their service/controller references idempotently after scope closure.
   - [x] Own Inventory Limit listener and guard delayed enforcement callbacks.
   - [x] Remove anonymous unowned registration from each migrated module without changing item or gameplay behavior.
   - Verification: no resource acquired by these modules lacks a matching scope action, stale callbacks are guarded, and focused tests pass.

6. Migrate Bounty and Airdrop without overwriting in-progress feature work
   - Files affected: `BountyModule.java`, `AirdropModule.java`, and focused lifecycle tests only.
   - [x] Re-read the latest Bounty file and diff immediately before patching.
   - [x] Retain/own both current Bounty listeners and restore prior executor/completer bindings for `bounty`, `bal`, and `withdraw` on close.
   - [x] Keep all payment-voucher, balance, withdrawal, and storage behavior untouched.
   - [x] Own Airdrop listener, interval/falling tasks, and the `airdrop` command binding; guard the landing callback.
   - [x] Make both module shutdown paths idempotent.
   - Verification: the diff contains only lifecycle wiring around the user's current Bounty implementation; Airdrop/Bounty behavior tests remain green.

7. Migrate infrastructure and presentation modules to explicit scopes
   - Files affected: Resource Pack, Teams, Nexus, Shop, PvE Boss, Geyser, Moderation, Server Control, Discord, and directly owned runtime/gateway classes only.
   - [x] Own Resource Pack listener and delayed retry/kick callbacks.
   - [x] Own Team listener and PlaceholderAPI expansion unregister action.
   - [x] Own all Nexus listeners including the anonymous placement listener, restore/attack/aura tasks, custom-item registration, gateway, models, entities, and client-presentation handles without changing current dirty placement-policy work.
   - [x] Own Shop listeners, spawned NPCs, and client-presentation entries.
   - [x] Own PvE runtime listeners/tasks, projectile `ItemDisplay` entities, cloned Wardens, boss/model/music/presentation shutdown, and guard delayed combat/removal callbacks.
   - [x] Prove PvE transient entities are removed through explicit ownership before the plugin-wide scheduler fallback can cancel their delayed removal tasks.
   - [x] Own Geyser event-bus subscription and guard queued main-thread callbacks without claiming dynamic content redefinition.
   - [x] Own Moderation, Server Control, and Discord listeners/tasks/log handlers/sinks/callback bindings/gateways; guard external and queued callbacks.
   - [x] Keep existing correct domain shutdown logic and avoid style-only rewrites.
   - Verification: every inventory row has explicit per-module cleanup, every touched shutdown is idempotent, and focused regression tests pass.

8. Add single-flight orchestration and structured results test-first
   - Files affected: new `reload` package and tests.
   - [x] Write failing tests for `SUCCESS`, `NO_OP`, `ALREADY_RUNNING`, and phase-specific `FAILED` results.
   - [x] Write a deterministic concurrent test proving only one caller executes and the other receives `ALREADY_RUNNING`.
   - [x] Write tests proving the in-progress flag resets after success and exception.
   - [x] Write a ten-cycle test proving every retired generation closes once, registration counts never accumulate, and only the newest callback mutates state.
   - [x] Include non-negative duration, generation IDs, ordered lifecycle outcomes, cleanup failures, warnings, and safe failure phase in immutable results.
   - [x] Keep coordinator tests pure and real Bukkit work synchronous on the primary server thread.
   - Verification: orchestration tests pass deterministically without a server or sleeps.

9. Integrate plugin reload/shutdown and truthful command output
   - Files affected: `BigCasares.java`, `BigCasaresCommand.java`, formatter, and command/formatter tests.
   - [x] Route initial activation, manual reload, failed activation cleanup, and plugin shutdown through the generation/module-scope lifecycle.
   - [x] Retire the old generation, disable modules and close scopes in reverse order, verify owned transient entities/presentations are removed, then execute temporary plugin-wide listener/task cleanup.
   - [x] Stop and return failure if retirement/cleanup fails; after config reload, tear down a failed new generation completely and leave no partial modules active.
   - [x] Preserve module order, config gates, command routing, resource-pack behavior, BetterModel construction, and Geyser/Discord boundaries.
   - [x] Write formatter tests for concise Spanish success, no-op, already-running, and failure messages.
   - [x] Return `ReloadResult` from `reloadPluginState()` and remove unconditional `Configs recargadas.` output.
   - [x] Keep `bigcasares.shop.reload` behavior unchanged in Phase 0.
   - Verification: no failed/already-running path emits green success; bootstrap and shutdown use the same idempotent ownership protocol.

10. Verify, document, and prepare the Phase 1 handoff
   - Files affected: tests and this plan's completion report only after implementation succeeds.
   - [x] Run focused scope, module-manager, reload, command, and every modified module test.
   - [x] Run existing resource-pack, Geyser, BetterModel/model, Nexus, boss, team, and shop tests.
   - [x] Run `GRADLE_USER_HOME=<writable-temp-path> bash gradlew test` without adding Gradle cache files to the repository.
   - [x] Run `GRADLE_USER_HOME=<writable-temp-path> bash gradlew clean build` as required by `GUIDELINES.MD`.
   - [x] Run `git diff --check` and inspect `git status --short` for unrelated or generated changes.
   - [x] Confirm ten-cycle coverage leaves one active set and all retired callbacks are inert.
   - [x] Confirm the diff contains no Phase 1+ content, pack, HTTP, item, sound, entity-definition, dependency, or asset changes.
   - [x] Mark only completed checkboxes and fill the completion report with actual files, tests, commands, deviations, limitations, branch, and temporary-fallback removal condition.
   - Verification: full tests/build pass, the completion report matches the diff, and Phase 1 remains a separate approval/implementation.

---

## Agent Completion Report

**Agent:** Codex (GPT-5)
**Date completed:** 2026-07-15
**Branch:** `main`

### What was built

- Runtime ownership core: `PluginModule`, `ModuleManager`, lifecycle outcome/report/status types, `RuntimeGeneration`, `RuntimeRegistrationScope`, cleanup result types, and `BukkitRuntimeRegistrations`.
- Reload orchestration: `ReloadCoordinator`, mandatory generation-bearing `ReloadOperation`, result/status/phase types, truthful Spanish formatting, required root-command validation, and `BigCasares` bootstrap/reload/shutdown integration.
- Gameplay ownership migrations: Copper Apple, Smoke Bomb, Custom Crossbow, Missions/HUD, Bounty command/listener wiring, Inventory Limit, Skill Rating, and Airdrop.
- Infrastructure ownership migrations: Resource Pack, Teams/PlaceholderAPI, Nexus/model/entity presentation, Shop GUI/NPC presentation, Geyser, Moderation, Server Control, and Discord/JDA callbacks.
- PvE ownership: the boss runtime plus explicit projectile-display, cloned-Warden, and associated task cleanup through `PveTransientOwner`.
- Documentation: investigation, design, implementation plan, handoff, and this completion report.
- Existing payment-voucher/Bounty domain changes and Nexus placement-policy changes in the dirty worktree were preserved and are not claimed as Phase 0 lifecycle work.

### Tests written

- `RuntimeRegistrationScopeTest` — reverse/idempotent cleanup, failure aggregation, stale guards, and latch-based retirement ordering.
- `BukkitRuntimeRegistrationsTest` — listener/task/recipe/command ownership, guarded scheduling, fallback cleanup, and failure-tolerant command restoration.
- `ModuleManagerLifecycleTest` — scoped activation, partial rollback, reverse teardown, failure reporting, idempotency, and retired-generation rejection.
- `ReloadCoordinatorTest` — single flight, phase failures, monotonic generation reservations, mandatory supplied generations, and deterministic ten-cycle behavior.
- `ReloadResultTest` and `ReloadMessageFormatterTest` — immutable result invariants and truthful terminal messages, including preparation and uncertain internal failures.
- `RequiredCommandBindingsTest` — complete root-command resolution and named missing-command failure.
- `PveTransientOwnerTest` — reverse transient entity/task cleanup, failure continuation, guarding, and idempotency.

### Testing instructions

Run focused Phase 0 tests:

```bash
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew test \
  --tests 'dev.linqfy.bigCasares.module.*' \
  --tests 'dev.linqfy.bigCasares.reload.*' \
  --tests 'dev.linqfy.bigCasares.command.BigCasaresCommandTest' \
  --tests 'dev.linqfy.bigCasares.command.RequiredCommandBindingsTest' \
  --tests 'dev.linqfy.bigCasares.modules.pveboss.*'
```

Run full verification:

```bash
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew test
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew clean build
git diff --check
```

Actual result on 2026-07-15: 315 tests passed with zero failures, errors, or skips; `clean build` passed and generated the existing resource packs with hash `80e75ffed95a`; `git diff --check` passed.

### Deviations from plan

- Concrete cleanup records are named `RuntimeCleanupOutcome`/`RuntimeCleanupReport` and lifecycle aggregates use `ModuleLifecycleReport`, rather than the provisional names in the file-structure sketch.
- Added `RequiredCommandBindings` so missing root commands fail before rebinding; the existing `/bigcasares` handler remains available as the retry path after a failed reload.
- Added monotonic attempted-generation reservation and strengthened runtime retirement beyond the original active-flag sketch so `retire()` waits for already-running guarded callbacks.
- Verification is deterministic unit/integration coverage plus the full Gradle build; no live Paper-server ten-cycle exercise was performed in Phase 0.

### Known limitations

- Phase 0 intentionally provides no transactional rollback and no runtime content/resource-pack generation.
- Cleanup resource IDs are logged at scope teardown but are not retained when failures are translated into module lifecycle outcomes.
- Public reload entry points do not yet enforce Bukkit primary-thread execution; current command-driven reloads run synchronously on the primary thread.
- Geyser callbacks queued onto Bukkit are generation-guarded; already-running external lifecycle-definition handlers still rely on subscription close/unregister.
- The plugin-wide Bukkit listener/task cleanup remains a temporary safety net. Remove it only after integration coverage proves every plugin-owned registration has explicit ownership and ten live reload cycles on Paper report no fallback-discovered listeners or tasks.
