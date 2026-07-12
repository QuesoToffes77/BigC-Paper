# Team Identity, Nexus State, and Protected Containers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add mutable styled Unicode-safe team identities, live team-bound Nexus presentation and attacker state, plus persistent team-owned Nexus containers.

**Architecture:** Keep `TeamId` as the only cross-module identity and centralize all visible formatting in the teams module. Add focused Nexus services for attacker tracking and protected-container persistence, with Bukkit listeners adapting events into testable domain operations.

**Tech Stack:** Java 21, Paper 26.2 API, Gradle 9.4.1, JUnit 5, Bukkit YAML configuration.

---

### Task 1: Unicode-safe tag value and appearance model

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamTagStyle.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamTagWrapper.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamTagValidator.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/Team.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamTagValidationTest.java`

- [ ] Add failing tests proving two-to-five grapheme validation, permitted configured symbols, and rejection of `§`, `&`, controls, bidi, zero-width text, and newlines.
- [ ] Run `./gradlew test --tests '*TeamTagValidationTest'` and confirm the new cases fail.
- [ ] Implement `TeamTagStyle` with six immutable properties and `TeamTagWrapper` templates; replace the ASCII regex with `BreakIterator`-based visible grapheme validation plus explicit symbol allowlisting.
- [ ] Extend `Team` with `tagStyle`, `withName`, and `withTagStyle`, preserving square/bold defaults for legacy construction.
- [ ] Run `./gradlew test --tests '*TeamTagValidationTest'` and confirm all cases pass.

### Task 2: Team persistence, rename, and centralized rendering

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamYamlMapper.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamService.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamPresentation.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamPresentationService.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamYamlMapperTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamServiceTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamPresentationServiceTest.java`

- [ ] Add failing tests for legacy YAML migration, full style round-trip, case-insensitive unique renames, owner authorization, and consistent styled rendering.
- [ ] Run the three targeted test classes and verify failures identify missing style/rename behavior.
- [ ] Persist `appearance.bold`, `italic`, `underlined`, `strikethrough`, `color`, and `wrapper`; supply legacy defaults when absent.
- [ ] Implement owner-only `renameTeam` and style updates, reusing normalized uniqueness checks for creation and rename.
- [ ] Centralize legacy-code rendering in `TeamPresentation` and make every presentation gateway consume it.
- [ ] Run the targeted team tests and confirm they pass.

### Task 3: Commands and configurable Unicode allowlist

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamModule.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/plugin.yml`
- Test: `src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamModuleWiringTest.java`

- [ ] Add failing command tests for `/team rename`, every appearance toggle, color, wrapper, invalid values, permissions, and tab completion.
- [ ] Run the two targeted test classes and confirm the new cases fail.
- [ ] Add `teams.tag.allowed-symbols`, Unicode-category switches, wrapper options, and Spanish response messages.
- [ ] Wire `/team appearance <bold|italic|underline|strikethrough|color|wrapper> <value>` and `/team rename <name>` to the team service.
- [ ] Run the targeted command/wiring tests and confirm they pass.

### Task 4: Live Nexus presentation by TeamId

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusTeamPresentationGateway.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusVisualRequest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModule.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/JavaNexusVisualGateway.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamPresentationListener.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusModuleWiringTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusVisualRecoveryTest.java`

- [ ] Add failing tests showing stored Nexus data contains only `TeamId` and that rename/tag/style events refresh existing visuals.
- [ ] Run targeted Nexus tests and confirm stale snapshot behavior fails them.
- [ ] Resolve current presentation through `NexusTeamPresentationGateway` on spawn/recovery/refresh and add a team-change refresh callback.
- [ ] Migrate existing Nexus YAML that contains copied `teamName` without losing health or position.
- [ ] Run targeted Nexus tests and confirm they pass.

### Task 5: Active attacker lifecycle

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusAttackTracker.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusAttackerStatus.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusListener.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModule.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/JavaNexusVisualGateway.java`
- Modify: `src/main/resources/config.yml`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusAttackTrackerTest.java`

- [ ] Add failing pure unit tests for multiple attackers, 60-block boundary, death, disconnect, world change, friendly/rejected damage, critical priority, and state transitions.
- [ ] Run `./gradlew test --tests '*NexusAttackTrackerTest'` and confirm it fails because the tracker is absent.
- [ ] Implement UUID sets per Nexus and a status-probe sweep that retains attackers only while online, alive, same-world, and within configured squared distance.
- [ ] Register attackers only after `NexusDamageOutcome.APPLIED`; schedule sweeps on the main thread and refresh visuals only when displayed state changes.
- [ ] Render `SEGURO`, `BAJO ATAQUE`, or `CRÍTICO` according to the approved priority and clear tracker state on Nexus removal.
- [ ] Run the tracker test and existing Nexus damage tests and confirm they pass.

### Task 6: Protected-container domain and YAML persistence

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusProtectedContainer.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerRegistry.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerStorage.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/YamlNexusContainerStorage.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerRegistryTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerStorageTest.java`

- [ ] Add failing tests for registration by TeamId/world/coordinates, double-chest halves, removal, Nexus/team cleanup, YAML round-trip, legacy adoption, and stale-record pruning.
- [ ] Run both new test classes and confirm they fail because registry/storage do not exist.
- [ ] Implement immutable container records and registry ownership queries without Bukkit dependencies.
- [ ] Implement atomic YAML save/load with schema versioning and deterministic ordering.
- [ ] Run both new test classes and confirm they pass.

### Task 7: Bukkit container protection events

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerListener.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusListener.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModule.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusPlacementPolicy.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerProtectionTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerClearanceTest.java`

- [ ] Add failing tests for own-team placement/open/break, enemy denial, explosion filtering, combustion cancellation, piston denial, double chests, and placement in another team's Nexus.
- [ ] Run targeted container tests and confirm current blanket placement denial fails own-team cases.
- [ ] Replace blanket denial with membership-aware registration and handle inventory interaction, block break, explosion, burn, piston, and world unload events at safe priorities.
- [ ] Persist after successful placement/break, adopt unambiguous containers on recovery, and remove metadata without deleting blocks when Nexus/team ownership ends.
- [ ] Run all Nexus container and placement tests and confirm they pass.

### Task 8: Regression verification and live smoke test

**Files:**
- Modify if needed: `README.md`

- [ ] Run `./gradlew clean test` and require zero failing tests.
- [ ] Run `./gradlew build` and require `BUILD SUCCESSFUL`.
- [ ] Start `./gradlew runServer`, accept the isolated test EULA if necessary, and wait for Paper's `Done` line.
- [ ] Exercise team create/rename/Unicode tag/style, Nexus damage lifecycle, restart persistence, and same-team/enemy container access using two test identities where feasible.
- [ ] Record exact automated results and any live-test limitation in the final handoff; do not claim unperformed client behavior.
