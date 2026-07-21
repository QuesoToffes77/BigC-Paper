# Tracker Compass and Nuke Shot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Add a 45-second player-tracking compass with a subsequent 60-second cooldown and a single-use fishing rod that builds ten expanding TNT rings 50 blocks above its user before dropping exactly 130 primed TNT.

**Architecture:** A new `special-items` module registers both catalog-backed items. Pure tracker state transitions and ring geometry remain Bukkit-free and testable, while thin listeners and runtimes own Bukkit events, inventory presentation, displays, scheduling, and TNT spawning.

**Tech Stack:** Java 21, Spigot API 26.2 / Minecraft 1.21.11, Gradle, JUnit 5, existing `PluginModule`, runtime-registration, and custom-item catalog infrastructure.

---

## File Structure

- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsModule.java` — module wiring and cleanup.
- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java` — validated configuration.
- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassItem.java` and `NukeShotItem.java` — catalog identities.
- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassService.java` and `TrackerCompassState.java` — timing/state transitions.
- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassListener.java` and `TrackerCompassRuntime.java` — event routing and live item reconciliation.
- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayout.java`, `NukeRing.java`, and `NukePoint.java` — deterministic geometry.
- `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeShotListener.java` and `NukeAnimationRuntime.java` — activation and entity animation.
- `src/main/resources/content/items/{tracker_compass,nuke_shot}.yml` — catalog definitions.
- `resourcepack/java/assets/bigcasares/items/{tracker_compass,nuke_shot}.json`, model JSON, Java textures, and matching Bedrock textures/registry entries — cross-platform appearances.
- Existing catalog loader/factory, module bootstrap, config, resource registry, language files, and catalog tests — integration points.

## Boundaries

- Preserve every unrelated dirty-worktree change.
- Do not change dependency or platform versions.
- Do not change existing item behavior or global TNT rules.
- Do not persist tracker state.
- Do not run `gradle clean`.
- Do not create recipes for either item.

### Task 1: Catalog durability support and item definitions

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/CustomItemDefinition.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogLoader.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/CatalogItemStackFactory.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogValidator.java`
- Modify: direct `CustomItemDefinition` constructor call sites under `src/test/java/dev/linqfy/bigCasares/items/catalog/`
- Create: `src/main/resources/content/items/tracker_compass.yml`
- Create: `src/main/resources/content/items/nuke_shot.yml`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/items/ItemCatalogModule.java`
- Modify/Test: `src/test/java/dev/linqfy/bigCasares/items/catalog/DefaultItemCatalogTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogLoaderTest.java`

- [x] Add failing loader/default-catalog tests asserting `tracker_compass`, `nuke_shot`, `max-stack-size: 1`, and `components.max-damage: 1` for Nuke Shot.
- [x] Run `gradle test --tests "dev.linqfy.bigCasares.items.catalog.DefaultItemCatalogTest" --tests "dev.linqfy.bigCasares.items.catalog.ItemCatalogLoaderTest"` and confirm failure because the new files and max-damage field are absent.
- [x] Add nullable `Integer maxDamage` to `CustomItemDefinition`, validate it is positive, parse `components.max-damage`, and apply it only when metadata implements `Damageable`:

```java
if (definition.maxDamage() != null) {
    if (!(meta instanceof Damageable damageable)) {
        throw new IllegalArgumentException("max-damage requires a damageable material: " + definition.material());
    }
    damageable.setMaxDamage(definition.maxDamage());
}
```

- [x] Define `tracker_compass` as `COMPASS`/model data `1007` and `nuke_shot` as `FISHING_ROD`/model data `1008`, both stack size one, with Nuke Shot max damage one and Spanish default names/lore.
- [x] Add both ID/mechanic pairs to the validator allowlist, seed both files from `ItemCatalogModule.DEFAULT_FILES`, update constructor fixtures with `null` max damage, rerun the focused tests, and confirm PASS.

### Task 2: Tracker state machine

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassState.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassService.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassServiceTest.java`

- [x] Write failing tests for idle activation, active-hit rejection, exact 45-second expiry into cooldown, early invalidation into cooldown, cooldown-hit rejection, exact 60-second expiry, and clear-all cleanup.
- [x] Run `gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.TrackerCompassServiceTest"` and confirm compilation failure because production types are absent.
- [x] Implement immutable state and explicit transitions. Use `UUID` keys and caller-supplied milliseconds:

```java
public boolean startTracking(UUID attacker, UUID target, long nowMillis);
public TrackerCompassState state(UUID attacker, long nowMillis);
public void invalidateTarget(UUID target, long nowMillis);
public void clear();
```

- [x] Ensure `state` changes expired tracking into a cooldown whose deadline is `trackingDeadline + cooldownMillis`, while early invalidation starts cooldown at invalidation time.
- [x] Rerun the focused test and confirm PASS.

### Task 3: Nuke ring geometry

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukePoint.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeRing.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayout.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayoutTest.java`

- [x] Write failing tests asserting ten nonempty rings, strictly increasing populations, exactly 130 unique rounded coordinates, increasing radii, and approximately even angular spacing.
- [x] Run `gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.NukeRingLayoutTest"` and confirm failure because the layout is absent.
- [x] Implement the balanced exact population sequence `4,6,8,10,12,14,16,18,20,22`, which is strictly increasing and totals 130.
- [x] Generate radius `2.0 * (ringIndex + 1)` and coordinates `(cos(angle) * radius, 0, sin(angle) * radius)` for each ring.
- [x] Rerun the focused test and confirm PASS.

### Task 4: Settings and module wiring

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassItem.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeShotItem.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsModule.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettingsTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsModuleWiringTest.java`

- [x] Write failing settings tests for confirmed defaults and invalid non-positive values, plus a wiring test asserting module id `special-items`.
- [x] Run both focused test classes and confirm failure because production types are absent.
- [x] Implement the validated settings record with tracking `45_000ms`, cooldown `60_000ms`, update `20 ticks`, height `50`, ring count `10`, total `130`, and interval `5 ticks` defaults loaded from `config.yml`.
- [x] Implement both catalog-backed item classes and module lifecycle registration/unregistration with the existing `RuntimeRegistrationScope` pattern.
- [x] Rerun focused tests and confirm PASS.

### Task 5: Tracker Bukkit runtime

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassListener.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassRuntime.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/TrackerCompassPresentationTest.java`

- [x] Write failing pure presentation tests for active Spanish name/lore, cooldown lore, rounded-up remaining seconds, and glint state.
- [x] Run the focused presentation test and confirm failure because the presentation function is absent.
- [x] Implement a package-private presentation record/function, then implement the runtime tick to resolve target validity and update every matching compass in the attacker's inventory.
- [x] Apply active metadata using `CompassMeta#setLodestone`, `setLodestoneTracked(false)`, `setEnchantmentGlintOverride(true)`, `setDisplayName`, and `setLore`; clear lodestone/glint and restore catalog defaults after cooldown.
- [x] Implement listener routing for main-hand custom-compass melee hits and target quit/death events. Active/cooldown hits send concise Spanish feedback without restarting state.
- [x] Register the listener and repeating runtime task through `BukkitRuntimeRegistrations`; own `service.clear()` cleanup.
- [x] Rerun tracker tests and confirm PASS.

### Task 6: Nuke animation runtime

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationRuntime.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeShotListener.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationTimelineTest.java`

- [x] Write a failing pure timeline test asserting seed at tick zero, rings at ticks `5..50`, and release at tick 50 with exactly 130 points.
- [x] Run the focused test and confirm failure because timeline logic is absent.
- [x] Implement timeline calculation and a Bukkit runtime that snapshots `player.getLocation().add(0, height, 0)`, spawns a central stationary TNT `BlockDisplay`, removes it on the first ring, then adds ring displays at five-tick intervals.
- [x] On the final step, remove all displays and spawn `TNTPrimed` at the same locations in one callback, with source set to the activating player when still valid and normal fuse/gravity behavior unchanged.
- [x] On any pre-release failure, remove already-created displays and log the exception; `close()` cancels unfinished animation tasks and removes only displays.
- [x] Implement a main-hand-only right-click listener that validates registry identity, cancels the vanilla interaction, removes exactly one held Nuke Shot before starting, plays buildup/release sounds, and never duplicates off-hand handling.
- [x] Register listener/runtime cleanup in `SpecialItemsModule`, rerun focused tests, and confirm PASS.

### Task 7: Bootstrap, configuration, and resource-pack integration

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/resources/config.yml`
- Create/Modify: Java item definitions, models, textures, and language files under `resourcepack/java/assets/bigcasares/`
- Create/Modify: Bedrock textures and `resourcepack/bedrock/textures/item_texture.json`
- Modify: `resourcepack/shared/registry.yml`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsResourcePackTest.java`

- [x] Write a failing resource test that verifies both catalog appearance paths, Java item/model JSON, Java and Bedrock PNG files, language keys, Bedrock atlas keys, and shared registry assets.
- [x] Run the focused resource test and confirm failure because pack assets are absent.
- [x] Register `SpecialItemsModule` immediately after the item catalog module, add `modules.special-items: true`, and add the confirmed `special-items` settings block.
- [x] Use the image-generation workflow to create two transparent pixel-art sources: a glowing target compass and a hazard-striped fishing rod. Downscale with nearest-neighbor sampling to validated 16x16 PNG files, then add corresponding modern Java item-definition/model JSON, translation entries, Bedrock copies/atlas keys, and shared registry entries.
- [x] Rerun resource and module wiring tests and confirm PASS.

### Task 8: Regression verification and completion report

**Files:**
- Modify: `docs/superpowers/plans/2026-07-18-special-items.md`

- [x] Run `gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.*" --tests "dev.linqfy.bigCasares.items.catalog.*"` and confirm zero failures.
- [ ] Run `gradle test` and confirm the complete suite passes. The suite currently has one unrelated custom-crossbow resource-model assertion failure.
- [x] Run `gradle build -x test` without `clean` and confirm `BUILD SUCCESSFUL`; the normal build remains blocked by that unrelated test.
- [x] Run `git diff --check` on feature paths and inspect `git diff` to ensure unrelated work was not modified.
- [x] Append the required Agent Completion Report with exact files, tests, commands, deviations, and known limitations.
- [ ] Commit only the special-items implementation, its plan, tests, catalog definitions, and resource-pack assets; overlapping pre-existing edits make automatic staging unsafe.

---

## Agent Completion Report

**Agent:** Codex (GPT-5)
**Date completed:** 2026-07-18
**Branch:** agent/oraxen-content-runtime

### What was built

- Added the `special-items` module with catalog-backed Tracker Compass and Nuke Shot items.
- Added deterministic tracker state, dynamic compass name/lore/glint/lodestone reconciliation, hit activation, target invalidation, and reload cleanup.
- Added deterministic ten-ring geometry totaling 130 TNT, a 2.5-second display buildup, simultaneous falling primed-TNT release, single-use rod consumption, sounds, and pre-release cleanup.
- Extended catalog definitions and stack creation with optional `components.max-damage` support.
- Added both catalog YAML definitions, module configuration/bootstrap, Java and Bedrock registry metadata, generated pixel-art textures, item definitions, models, and translations.
- Added the approved design and implementation documents under `docs/superpowers/`.

### Tests written

- `TrackerCompassServiceTest` — tracking/cooldown transitions, rejection rules, invalidation, expiry, and cleanup.
- `TrackerCompassPresentationTest` — active and cooldown names, lore, rounded time, and glint.
- `NukeRingLayoutTest` — ten increasing rings, exact 130-point total, unique geometry, radii, and angular spacing.
- `NukeAnimationTimelineTest` — seed, five-tick ring cadence, and tick-50 release.
- `SpecialItemsSettingsTest` — confirmed defaults and invalid configuration.
- `SpecialItemsModuleWiringTest` — stable module id.
- `SpecialItemsResourcePackTest` — dedicated Java/Bedrock models, textures, translations, atlas, and registry entries.
- Extended catalog loader/default tests for both items and one-point Nuke Shot maximum damage.

### Testing instructions

```powershell
$env:GRADLE_USER_HOME = (Resolve-Path '.gradle-user-home-task5').Path
.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.specialitems.*" --tests "dev.linqfy.bigCasares.items.catalog.DefaultItemCatalogTest" --tests "dev.linqfy.bigCasares.items.catalog.ItemCatalogLoaderTest"
.\gradlew.bat generateResourcePacks
.\gradlew.bat build -x test
```

Expected result: all feature tests pass, both resource packs generate with the new assets, and packaging succeeds.

### Deviations from plan

- Used legacy model data `1007` and `1008` because the initially planned `1003` and `1004` are already occupied by Prismarine Arrow and Nexus.
- The complete test suite is not green because pre-existing dirty custom-crossbow models inherit `bigcasares:item/crossbow_arrow` while their edited test expects `minecraft:item/crossbow_arrow`. This feature does not modify those files.
- A normal `gradle build` would rerun the unrelated failing test, so packaging verification used `gradle build -x test` after fresh focused tests.
- No implementation commit was created because several integration files already contained unrelated uncommitted user changes; staging entire files would mix ownership.

### Known limitations

- Tracker state is intentionally in memory and does not persist through a restart.
- Tracking ends early across worlds, on target death, or on target disconnect.
- Released TNT uses normal server fuse, explosion, and protection behavior.
- Neither item has a crafting recipe because none was specified.
- Live destructive TNT behavior was not exercised on the retained local server world; geometry, timing, compilation, and pack outputs are automated.

