# Transactional Java Pack Hot Reload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Implement Phase 1 from the Oraxen-inspired investigation: seed editable runtime pack sources, prepare and validate candidates in isolated staging directories, derive identity from final merged Java bytes, atomically publish an active content-addressed manifest, optionally serve it through embedded HTTP, and expose truthful reload/info/send commands without adding declarative content catalogs.

## Tech stack

- Existing BigCasares Java/Paper/Gradle baseline
- JUnit 5.11.4
- JDK `HttpServer`; no new dependencies

## Boundaries

- Preserve Phase 0 lifecycle behavior and all unrelated Bounty/Nexus work.
- Do not add Phase 2 item definitions, identity migration, recipes, or reconciliation.
- Do not claim live Bedrock or BetterModel redefinition.
- Do not overwrite operator-edited runtime content when seeding defaults.
- Do not change current repository pack assets or dependency versions.

## Tasks

1. Seed runtime pack content reproducibly
   - [x] Add a reproducible Gradle seed ZIP and embed it in the plugin JAR.
   - [x] Test safe extraction, missing-only copies, idempotency, and traversal rejection.
   - [x] Integrate seeding under `content/pack` without requiring a live server.

2. Validate complete candidate inputs and archives
   - [x] Add strict JSON syntax and PNG readability checks for source and ZIP entries.
   - [x] Reject unsafe archive paths, duplicate entries, malformed metadata, and missing registry references.
   - [x] Keep validation deterministic and independent of Bukkit.

3. Build staged candidates with final-byte identity
   - [x] Add explicit ordered Java layer providers and strict BetterModel merging.
   - [x] Build every job in a unique staging directory.
   - [x] Derive Java SHA-1/SHA-256, UUID, version, and Bedrock SHA-256 after all merges.
   - [x] Prove BetterModel-only changes alter identity and identical final bytes are stable.
   - [x] Clean staging on success, no-op, and every failure.

4. Publish immutable artifacts and active manifest
   - [x] Publish hash-addressed Java and Bedrock filenames without overwriting different bytes.
   - [x] Atomically replace `active-pack.json` only after candidate validation.
   - [x] Preserve the previous artifact and manifest on pre-commit failure.
   - [x] Test manifest round-trip, no-op comparison, and failed-candidate rollback.

5. Add embedded HTTP delivery
   - [x] Parse explicit bind address, port, and public base URL settings.
   - [x] Stream only content-addressed artifact paths with immutable headers.
   - [x] Reject traversal, directories, unknown files, and unsupported methods.
   - [x] Prove old artifact downloads remain valid after an active swap and shutdown releases the port.

6. Coordinate and integrate pack reload
   - [x] Add single-flight structured pack reload phases/results/status.
   - [x] Run preparation on one worker and commit/resend on the Bukkit primary thread.
   - [x] Prevent a retired module from committing a finished candidate.
   - [x] Update `ResourcePackService` atomically and ignore stale prior-pack status mutations.
   - [x] Own worker/server/callback lifecycle through the module registration scope.

7. Add command and permission surface
   - [x] Add `/bigcasares reload pack`, `/bigcasares pack info`, and `/bigcasares pack send <player|all>`.
   - [x] Add concise Spanish structured messages and tab completion.
   - [x] Add pack-specific permissions while retaining the old reload permission as an alias.

8. Verify and document
   - [x] Run focused resource-pack, reload, command, and lifecycle tests.
   - [x] Run the full test suite and `clean build` with a writable Gradle home.
   - [x] Run `git diff --check` and audit generated/unrelated changes.
   - [x] Fill the completion report with actual results and limitations.

---

## Agent Completion Report

**Agent:** Codex (GPT-5)
**Date completed:** 2026-07-15
**Branch:** `main`

### What was built

- A reproducible embedded seed archive initializes missing runtime sources under `content/pack` without overwriting operator edits.
- `StagedPackBuilder` now performs isolated Java/Bedrock builds, strict ordered Java-layer merging, complete archive validation, final-byte hashing/identity, and terminal staging cleanup.
- `PackArtifactPublisher` publishes immutable hash-addressed artifacts, validates active artifacts, and atomically swaps `active-pack.json` while retaining old downloads.
- `EmbeddedPackHttpServer` provides exact-path GET/HEAD streaming with immutable cache headers, explicit bind/public URL settings, and lifecycle-owned shutdown.
- `PackReloadCoordinator` provides single-flight asynchronous preparation, primary-thread commit, structured phase results, no-op handling, and retirement protection.
- `ResourcePackModule` seeds, builds or restores, publishes, activates, and resends the Java pack; stale prior-pack status events cannot mutate current state.
- `/bigcasares reload pack`, `pack info`, and `pack send <player|all>` were added with pack-specific permissions and the legacy reload permission alias.

### Tests written

- Added focused suites for runtime seed extraction, final archive validation, staging/final identity, immutable publication, embedded HTTP, pack reload coordination, message formatting, and pack transitions.
- Extended command and service tests for the new command surface, stale pack IDs, forced manual sends, no-op behavior, and missing active artifacts.
- Added a repository-source test that runs the real `resourcepack/` tree through the runtime staging and archive validators.

### Verification

- Focused resource-pack and command regression run: 42 tests, zero failures/errors/skips.
- Final `bash gradlew clean build`: successful; all 338 tests passed with zero failures/errors/skips.
- The clean build generated both existing packs at hash `80e75ffed95a` and embedded both `generated-resourcepacks/manifest.json` and `seed-content/bigcasares-content-seed.zip` in the plugin JAR.
- `git diff --check` passed. Generated build output remained under ignored `build/`; existing dirty Bounty and Nexus work was preserved.

### Deviations and limitations

- Phase 1 deliberately does not add declarative items, recipes, sounds, entity catalogs, reconciliation, or identity migration; those remain later phases.
- Bedrock artifacts are rebuilt and versioned, but dynamic Geyser application remains restart-required.
- BetterModel contributes only through an explicitly configured generated Java-pack archive; BetterModel commands/models are not dynamically reloaded.
- Embedded serving is plain HTTP and expects an operator-provided public base URL plus external TLS/proxying when required.
- Verification is deterministic unit/integration coverage and a clean build. No live Paper client download/resend exercise or live ten-cycle reload exercise was performed; the Phase 0 temporary fallback therefore remains in place.
