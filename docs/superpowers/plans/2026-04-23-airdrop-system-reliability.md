# 2026-04-23-airdrop-system-reliability.md

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Repair the production `airdrop-system` module without changing its overall shape. The work tightens runtime correctness, persistence, lifecycle handling, and test coverage while preserving the existing service/module/listener/storage split.

## Tech Stack

- Java 21
- Paper 1.21.11 (`spigot-api:1.21.11-R0.1-SNAPSHOT`)
- Gradle (Groovy DSL)
- JUnit 5 (`junit-bom:5.11.4`)

## File Structure

| File | Description |
|---|---|
| `docs/superpowers/specs/2026-04-23-airdrop-system-reliability-design.md` | Reliability-focused design spec for the fixes |
| `docs/superpowers/plans/2026-04-23-airdrop-system-reliability.md` | This implementation plan and completion report |
| `AirdropPhase.java` | Explicit persisted drop phase |
| `AirdropData.java` | Domain state updated to use phase instead of hidden active flag |
| `AirdropPosition.java` | Block-coordinate helper methods for safe matching |
| `AirdropLootEntry.java` | Defensive validation for immutable loot entries |
| `AirdropLootMaterialResolver.java` | Bukkit-side fail-fast material validation |
| `AirdropWorldGateway.java` | Safer world contract for landing validation |
| `BukkitAirdropWorldGateway.java` | Explicit safe-ground and safe-space logic |
| `AirdropService.java` | Pure state machine for spawn, landed, claimed, and restore-ready state |
| `AirdropListener.java` | Block-coordinate claim logic with prevalidated materials |
| `AirdropFallingTask.java` | Landing callback and safer task cleanup hooks |
| `AirdropModule.java` | Restore logic, task ownership, scheduler cleanup, loot validation |
| `YamlAirdropStorage.java` | Persist/load `phase` with backward compatibility |
| `ModuleManager.java` | Support both nested and flat `enabled` config styles |
| `AirdropPositionTest.java` | Block-coordinate comparison tests |
| `AirdropLootMaterialResolverTest.java` | Material validation failure test |
| `AirdropServiceTest.java` | Retry, location validation, and phase transition coverage |
| `YamlAirdropStorageTest.java` | Phase persistence coverage |
| `AirdropModuleWiringTest.java` | Stable module id via direct module instantiation |
| `ModuleManagerTest.java` | Nested module enabled-key integration coverage |

## Boundaries

- Keep Bukkit code out of `AirdropService`
- Keep the airdrop module package structure intact
- Do not remove existing tests
- Do not use mocks or mocking frameworks
- Do not change unrelated modules or dependency versions

## Tasks

### Task 1 - Persisted domain state
- [x] Add `AirdropPhase`
- [x] Update `AirdropData` and `AirdropService` to use explicit phase transitions
- [x] Remove hidden null state from service internals
- Verification: `AirdropServiceTest` covers `FALLING -> LANDED -> CLAIMED`

### Task 2 - Safer world and loot validation
- [x] Replace passable-only terrain validation with safe-ground and safe-space checks
- [x] Explicitly reject lava landing blocks
- [x] Validate loot materials with `Material.matchMaterial()` during module enable
- Verification: service retry tests and material validation tests pass

### Task 3 - Bukkit lifecycle hardening
- [x] Remove `Location.equals()` usage in claim detection
- [x] Centralize falling-task ownership in `AirdropModule`
- [x] Restore falling/landed airdrops after restart
- [x] Cancel interval and falling tasks on disable
- Verification: code compiles and module wiring tests remain green

### Task 4 - Storage and integration
- [x] Persist `phase` in YAML and keep backward compatibility for old records
- [x] Support `modules.<id>.enabled` in `ModuleManager`
- Verification: storage and module manager tests pass

### Task 5 - Final validation
- [x] Run `./gradlew test`
- [x] Run `./gradlew clean build`
- [x] Complete the Agent Completion Report

---

## Agent Completion Report

**Agent:** GPT-5 Codex
**Date completed:** 2026-04-23
**Branch:** unknown

### What was built
- `docs/superpowers/specs/2026-04-23-airdrop-system-reliability-design.md`
- `docs/superpowers/plans/2026-04-23-airdrop-system-reliability.md`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropPhase.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropData.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropPosition.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropLootEntry.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropLootMaterialResolver.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropWorldGateway.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/BukkitAirdropWorldGateway.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropService.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropListener.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropFallingTask.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/AirdropModule.java`
- `src/main/java/dev/linqfy/bigCasares/modules/airdrop/YamlAirdropStorage.java`
- `src/main/java/dev/linqfy/bigCasares/module/ModuleManager.java`
- `src/test/java/dev/linqfy/bigCasares/modules/airdrop/AirdropPositionTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/airdrop/AirdropLootMaterialResolverTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/airdrop/AirdropServiceTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/airdrop/YamlAirdropStorageTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/airdrop/AirdropModuleWiringTest.java`
- `src/test/java/dev/linqfy/bigCasares/module/ModuleManagerTest.java`

### Tests written
- `AirdropPositionTest` - verifies exact block-coordinate matching instead of `Location.equals()`
- `AirdropLootMaterialResolverTest` - verifies valid material resolution and fail-fast invalid material handling
- `AirdropServiceTest` - verifies safe location retries, restart-safe phase transitions, and persistence updates
- `YamlAirdropStorageTest` - verifies phase round-trip storage and legacy `active` migration
- `AirdropModuleWiringTest` - verifies stable `airdrop-system` module id
- `ModuleManagerTest` - verifies nested `modules.<id>.enabled` integration behavior

### Testing instructions
How to run the tests for this module:
```
./gradlew test --tests "dev.linqfy.bigCasares.modules.airdrop.*"
```
Expected result: all tests pass, no failures.

### Deviations from plan
- Added `ModuleManagerTest` because the nested `enabled` key bug was part of the module integration issue, even though the class lives outside the airdrop package.
- The repository did not include `gradlew` or `gradlew.bat`, so validation was executed with a temporary local Gradle 9.1.0 distribution against the same project sources.

### Known limitations
- Falling-drop restoration resumes from the persisted spawn position rather than the exact in-flight Y from the last tick.
