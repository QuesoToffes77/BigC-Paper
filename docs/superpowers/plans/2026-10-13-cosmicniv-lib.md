# CosmicNiv Lib Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Add the `cosmicniv-lib` module: a shared library inside BigCasares that maps a player's (or mob's) armor, best
weapon and best tool to an equipment level from 0 to 200. How the level is computed (references, weights, curve)
is read from `plugins/BigCasares/cosmicniv_lib.yml`, and what everything is worth (armor, weapons, tools,
enchantments and custom items) from `plugins/BigCasares/cosmicniv_valores.yml`, so admins tune it without code.
Other modules call `getCosmicNivModule()`. Design: `docs/superpowers/specs/2026-10-13-cosmicniv-lib-design.md`.

## Tech Stack

- Paper API 26.2.build.56-alpha, Java 25, Gradle Wrapper 9.2.1.
- Unit tests: JUnit 5 against `spigot-api:26.2-R0.1-SNAPSHOT` (no new dependencies).

## File Structure

New (`src/main/java/dev/linqfy/bigCasares/modules/cosmicniv/`):
- `CosmicNivModule.java`: module lifecycle, two-file load with fallback, public API.
- `CosmicNivService.java`: math model and custom item recognition (no Bukkit).
- `CosmicNivSettings.java`, `CosmicNivSettingsLoader.java`, `CosmicNivConfigLoadResult.java`: config.
- `CosmicNivCurve.java`, `CosmicNivCurveType.java`: curves.
- `CosmicNivCustomItem.java`, `CosmicNivCustomMatch.java`, `CosmicNivItemIdentity.java`: custom items.
- `CosmicNivItem.java`, `CosmicNivLoadout.java`, `CosmicNivArmorSlot.java`, `CosmicNivItemCategory.java`,
  `CosmicNivReference.java`, `CosmicNivEnchantmentValue.java`, `CosmicNivHeldSearch.java`, `CosmicNivResult.java`,
  `CosmicNivKeys.java`: value types.
- `BukkitCosmicNivEquipmentReader.java`: `LivingEntity` to loadout.
- `BukkitCosmicNivKeyAudit.java`: warnings for keys unknown to the server.
- `CosmicNivCommand.java`: `/cosmicniv`.
- `src/main/resources/cosmicniv_lib.yml` and `src/main/resources/cosmicniv_valores.yml`: bundled configuration.
- Tests (`src/test/java/dev/linqfy/bigCasares/modules/cosmicniv/`): `CosmicNivCurveTest`,
  `CosmicNivSettingsLoaderTest`, `CosmicNivServiceTest`, `CosmicNivCustomItemTest`, `CosmicNivModuleWiringTest`.
- `docs/cosmicniv-lib/README.md`: Spanish user and developer guide.

Modified:
- `BigCasares.java`: field, getter, construction (with the previous instance's settings) and registration.
- `config.yml`: `modules.cosmicniv-lib.enabled: true`.
- `plugin.yml`: `/cosmicniv` (alias `/cniv`), `bigcasares.cosmicniv.*` permissions, admin child of `bigcasares.*`.
- `README.md`: short section linking to the guide.

## Boundaries

- Do not touch `build.gradle`, other modules or the resource pack.
- No persistence, no scheduled tasks, no Adventure or Paper-only classes loaded by tests.

## Tasks

### Task 1: Curves
Files: `CosmicNivCurve.java`, `CosmicNivCurveType.java`, `CosmicNivCurveTest.java`.
- [x] Write tests: every curve maps 0 to 0 and 1 to 1, is non-decreasing, matches its formula at sample points,
      clamps inputs outside [0,1] and rejects invalid or near-zero parameters.
- [x] Implement the sealed interface with one record per curve.
- Verification: `CosmicNivCurveTest` passes.

### Task 2: Value types and two-file settings loader
Files: value types, `CosmicNivSettings*.java`, `CosmicNivConfigLoadResult.java`, both bundled YAML files,
`CosmicNivSettingsLoaderTest.java`.
- [x] Write tests: the bundled files load without errors or warnings; every invalid case reports
      `<file> > <path>`; unknown keys, duplicates and out-of-range values are errors; parts with weight 0 need no
      references.
- [x] Implement the loader and write the bundled files from the vanilla 26.2 enchantment data.
- Verification: `CosmicNivSettingsLoaderTest` passes.

### Task 3: Service and custom items
Files: `CosmicNivService.java`, `CosmicNivResult.java`, custom item classes, `CosmicNivServiceTest.java`,
`CosmicNivCustomItemTest.java`.
- [x] Write tests: nothing = 0, full netherite + each max weapon = 200, weight split, tools, per-family
      normalization, caps, curses, custom zero reference, curves, rounding, custom item matching and the numbers
      printed in the guide.
- [x] Implement the service.
- Verification: both test classes pass.

### Task 4: Module, Bukkit adapters and command
Files: `CosmicNivModule.java`, `BukkitCosmicNiv*.java`, `CosmicNivCommand.java`, `CosmicNivModuleWiringTest.java`,
`BigCasares.java`, `config.yml`, `plugin.yml`.
- [x] Write the wiring test: stable id, unavailable before enable, settings carried to a new instance, bundled
      files, plugin.yml and BigCasares declarations.
- [x] Implement the module with the fallback, the reader, the key audit and the command.
- [x] Register the module, its config key, command and permissions.
- Verification: `CosmicNivModuleWiringTest` passes.

### Task 5: Docs, review and build
Files: `docs/cosmicniv-lib/README.md`, `README.md`, this plan.
- [x] Write the Spanish guide (usage, math, every key of both files, custom items, curves, API).
- [x] Independent read-only reviews (code and docs) and their fixes.
- [x] Run `./gradlew clean build`.
- [x] Add the Agent Completion Report.
- Verification: `BUILD SUCCESSFUL`.

---

## Agent Completion Report

**Agent:** Claude Opus 5.5 (Claude Code); read-only reviews by Claude Opus (code) and Claude Sonnet (docs) subagents
**Date completed:** 2026-10-14
**Branch:** agent/cosmicniv-lib

### What was built
- `src/main/java/dev/linqfy/bigCasares/modules/cosmicniv/`: `CosmicNivModule`, `CosmicNivService`,
  `CosmicNivSettings`, `CosmicNivSettingsLoader`, `CosmicNivConfigLoadResult`, `CosmicNivCurve`,
  `CosmicNivCurveType`, `CosmicNivCustomItem`, `CosmicNivCustomMatch`, `CosmicNivItemIdentity`, `CosmicNivItem`,
  `CosmicNivLoadout`, `CosmicNivArmorSlot`, `CosmicNivItemCategory`, `CosmicNivReference`,
  `CosmicNivEnchantmentValue`, `CosmicNivHeldSearch`, `CosmicNivResult`, `CosmicNivKeys`,
  `BukkitCosmicNivEquipmentReader`, `BukkitCosmicNivKeyAudit`, `CosmicNivCommand`.
- `src/main/resources/cosmicniv_lib.yml` (how the level is computed) and `src/main/resources/cosmicniv_valores.yml`
  (what everything is worth; reference enchantments from vanilla 26.2 data).
- `docs/cosmicniv-lib/README.md`: Spanish user and developer guide.
- `docs/superpowers/specs/2026-10-13-cosmicniv-lib-design.md` and this plan.
- Modified: `BigCasares.java`, `config.yml`, `plugin.yml`, `README.md`.

### Tests written
- `CosmicNivCurveTest` (13): curve endpoints, monotonicity, formulas, clamping and invalid parameters.
- `CosmicNivSettingsLoaderTest` (31): bundled files, defaults, curve types, and every validation error and warning
  with its file and path (unknown keys, text types, duplicates, limits, references, families, custom items, weight-0
  parts, MAIN_HAND warning).
- `CosmicNivServiceTest` (23): 0 and 200 references, weights, tools, families, caps, curses, curves, rounding,
  weight-0 parts, hand-built limits and the numbers printed in the guide.
- `CosmicNivCustomItemTest` (8): custom item recognition (catalog id, item model, persistent data, material), first
  match wins, where custom armor and tools count, invalid definitions.
- `CosmicNivModuleWiringTest` (8): stable id, availability, settings carried across reloads, bundled files,
  config key, plugin.yml, BigCasares registration, command formatting.

### Testing instructions
How to run the tests for this module:
```
./gradlew test --tests "dev.linqfy.bigCasares.modules.cosmicniv.*"
```
Expected result: all tests pass, no failures. Full `./gradlew clean build`: BUILD SUCCESSFUL, 1061 tests, 0
failures.

### Deviations from plan
- No `CosmicNivStorage` / `YamlCosmicNivStorage` / `CosmicNivListener`: the level is computed on demand and nothing
  is persisted or listened to, so the GUIDELINES storage and listener classes do not apply.
- The values moved from `cosmicniv_lib.yml` to a dedicated `cosmicniv_valores.yml`, tools were added as a third part
  (weight 0 by default) and custom items were added, at the user's request.
- Review fixes: minimum curve strength, ±1,000,000 limits (also enforced for hand-built settings), unknown keys and
  duplicate keys are errors, text-only match values, weapons never count as armor, enchantments summed in key order,
  settings carried across `/bigcasares reload`, weight-0 parts need no references and never affect the level,
  MAIN_HAND warning, command output with the configured decimals.

### Known limitations
- Not tested on a live server: the unit tests cover the model, the loader and the wiring; the Bukkit reader, the key
  audit and the command were verified by compiling and by review only.
- Durability, potion effects, item attribute modifiers, bows and crossbows are not counted by default (bows can be
  added as a weapon family in config).
- Spear base values are an approximate scale; tune them in-game.
- Players named `reload`, `curva` or `curve` cannot be looked up by name with `/cosmicniv`.
- Existing power formulas (Jeremy, mob scaling) were not migrated to this library.
