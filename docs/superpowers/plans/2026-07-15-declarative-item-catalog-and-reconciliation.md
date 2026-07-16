# Declarative Item Catalog and Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Implement Phase 2 of the Oraxen-inspired program: load four custom-item definitions
from editable runtime YAML, use a stable canonical PDC identity, retain legacy reads,
rebuild catalog-owned recipes, and reconcile online inventories conservatively without
moving gameplay mechanics out of their existing modules.

## Tech stack

- Java 25, Paper 26.2 / Spigot API test baseline
- Gradle Groovy build
- JUnit 5.11.4
- Existing YAML configuration API; no new dependencies

## File structure

### New production files

- `src/main/java/dev/linqfy/bigCasares/items/catalog/CustomItemDefinition.java` —
  immutable item, component, recipe, and appearance definition.
- `src/main/java/dev/linqfy/bigCasares/items/catalog/CustomItemCatalog.java` —
  immutable, revisioned item map.
- `src/main/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogLoader.java` and
  `ItemCatalogValidator.java` — YAML parsing and candidate validation.
- `src/main/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogSeeder.java` —
  non-destructive default-definition seeding.
- `src/main/java/dev/linqfy/bigCasares/items/catalog/CatalogItemStackFactory.java` —
  canonical PDC identity and catalog appearance application.
- `src/main/java/dev/linqfy/bigCasares/items/catalog/CatalogRecipeRuntime.java` —
  reversible owned recipe replacement.
- `src/main/java/dev/linqfy/bigCasares/items/catalog/OnlineItemReconciler.java` and
  result types — bounded, generation-guarded player inventory migration.
- `src/main/java/dev/linqfy/bigCasares/modules/items/ItemCatalogModule.java` —
  lifecycle-owned catalog/reload/recipe/reconciliation wiring.

### Modified production files

- `BigCasares.java` — construct the registry with canonical keys, register the catalog
  module before mechanic modules, and expose it to commands.
- `CustomItem.java` and `CustomItemRegistry.java` — catalog-first identity lookup plus
  legacy alias compatibility.
- The four custom-item classes — delegate stack appearance/identity to the catalog,
  retaining only their Java mechanics and legacy aliases.
- Copper Apple, Smoke Bomb, Custom Crossbow, and Nexus modules — use catalog-backed
  handlers; remove their direct recipe ownership.
- `BigCasaresCommand.java`, `config.yml`, and `plugin.yml` — item reload/info surface,
  permissions, and catalog module settings.
- `src/main/resources/content/items/*.yml` — package the four default editable
  definitions into the plugin JAR through the existing resource-processing task.

### Tests

- `items/catalog/*Test` — parsing, deterministic revisions, validation failures,
  seeding, canonical/legacy identity, component application, recipe rollback, and
  reconciliation preservation/removal policy.
- `modules/items/ItemCatalogModuleWiringTest` — stable module ID and reload boundary.
- Existing item/module/shop tests updated to use catalog-backed items.

## Boundaries

- Phase 2 only: do not add sound catalogs, entity presentation reconciliation, Geyser
  dynamic redefinition, generic mechanics, or pack host changes.
- Do not alter existing item mechanics, balances, recipes, or runtime pack assets.
- Preserve unrelated dirty worktree changes.
- Use TDD, immutable values, no Mockito, no wildcard imports, and no new dependencies.

## Tasks

1. Establish definitions and candidate validation
   - [x] Add failing tests for a valid four-item catalog, duplicate IDs/models,
     unsupported mechanics, invalid materials/recipes, unsafe IDs, and missing assets.
   - [x] Add the immutable catalog/definition values and deterministic revision.
   - [x] Implement YAML loader and validator with no live Bukkit server requirement.
   - [x] Add runtime default seeding that preserves operator edits.
   - Verification: invalid candidates cannot produce a catalog; seeded defaults parse.

2. Add canonical item identity and presentation factory
   - [ ] Add failing tests for canonical ID lookup, legacy model/PDC aliases, duplicate
     alias rejection, and canonical stack creation.
   - [x] Implement `bigcasares:item_id`/revision writing and legacy reads.
   - [x] Implement catalog-owned material, model, display, lore, food, and stack-limit
     application while retaining unrelated PDC.
   - [x] Migrate the four existing custom item classes to catalog-backed handlers.
   - Verification: old and new stacks resolve to the same logical ID.

3. Commit catalog-owned recipes transactionally
   - [x] Add failing fake-gateway tests for initial install, replacement, and rollback.
   - [x] Build shaped recipes from validated definitions.
   - [x] Move the three existing recipes from mechanic modules to the catalog runtime.
   - Verification: a failed recipe install leaves the prior recipe/catalog active.

4. Reconcile online inventories conservatively
   - [ ] Add failing fake-inventory tests for appearance refresh, state/PDC/name/lore
     preservation, legacy upgrade, removed-definition marking, batching, and retirement.
   - [x] Implement a generation-guarded reconciler for online player inventories and
     ender chests.
   - [x] Never delete removed or unrecognized stacks; report them as legacy.
   - Verification: player-owned mutable state survives a successful reconciliation.

5. Wire lifecycle, commands, and reload
   - [x] Add the catalog module before item mechanic modules and its config gate.
   - [x] Commit only fully validated candidates, then replace recipes and reconcile.
   - [x] Add `/bigcasares reload items` and `/bigcasares items info` with structured
     Spanish messages and permissions.
   - [x] Prove failed loads retain the current catalog and reloads are single-flight.
   - Verification: commands accurately distinguish success, no-op, already-running,
     and failure.

6. Verify and document
   - [x] Run focused catalog, item-mechanic, shop, command, resource-pack, and
     lifecycle tests.
   - [x] Run `GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew test` and
     `GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew clean build`.
   - [x] Run `git diff --check`, inspect the dirty worktree, and complete the report
     below with actual results and limitations.
   - [x] Run the catalog command smoke test on an isolated Paper server with BetterModel:
     verify initial info, no-op, successful candidate commit, failed-candidate retention,
     restoration, and clean shutdown.
   - Verification: all tests/build checks pass and Phase 3/4 scope remains untouched.

---

## Agent Completion Report

**Agent:** Codex (GPT-5)
**Date completed:** 2026-07-16
**Branch:** `main`

### What was built

- Added the immutable YAML catalog, candidate validator, deterministic revision,
  non-destructive default seeding, item identity/appearance factory, transactional
  recipe runtime, reload coordinator, and online-inventory reconciliation queue under
  `items/catalog/`.
- Added `ItemCatalogModule` and four packaged runtime definitions for Copper Apple,
  Smoke Bomb, Prismarine Arrow, and Nexus.
- Migrated the four item classes to catalog-backed presentation while retaining their
  Java mechanics and legacy marker/model-data compatibility aliases.
- Moved Copper Apple, Smoke Bomb, and Prismarine Arrow recipe creation from their
  mechanic modules into the catalog-owned transactional recipe runtime.
- Added `reload items` and `items info` command surfaces, config gate, and permissions.
- Made the existing resource-pack JSON validator reusable for catalog asset validation.
- Moved BigCasares to Paper's `POSTWORLD` load phase so the required BetterModel Paper
  plugin is enabled before BigCasares initializes its model-backed runtime.

### Tests written

- `ItemCatalogLoaderTest` — parses valid definitions and rejects duplicate, unsafe, and
  incomplete YAML.
- `ItemCatalogValidatorTest` — checks mechanic/material/model/recipe uniqueness and
  Java JSON plus Bedrock PNG appearance assets.
- `ItemCatalogSeederTest` and `DefaultItemCatalogTest` — prove non-destructive seeding
  and validate the four packaged defaults against the repository pack.
- `CatalogRecipeRuntimeTest` — verifies recipe replacement and rollback using a fake
  gateway.
- `ItemReconciliationPolicyTest` — covers refresh, legacy-mark, and ignore policy.
- `ItemCatalogReloadCoordinatorTest` and `ItemCatalogReloadMessageFormatterTest` —
  cover no-op, failure retention, single-flight, and Spanish failure output.
- `ItemCatalogModuleWiringTest` and updated `BigCasaresCommandTest` — cover module ID
  and catalog command suggestions.

### Testing instructions

Run focused coverage:

```bash
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew test \
  --tests 'dev.linqfy.bigCasares.items.catalog.*' \
  --tests 'dev.linqfy.bigCasares.modules.items.*' \
  --tests 'dev.linqfy.bigCasares.command.*' \
  --tests 'dev.linqfy.bigCasares.modules.copperapple.*' \
  --tests 'dev.linqfy.bigCasares.modules.smokebomb.*' \
  --tests 'dev.linqfy.bigCasares.modules.customcrossbow.*' \
  --tests 'dev.linqfy.bigCasares.modules.nexus.*' \
  --tests 'dev.linqfy.bigCasares.modules.shop.*' \
  --tests 'dev.linqfy.bigCasares.modules.resourcepack.*'
```

Final verification run on 2026-07-16:

```bash
GRADLE_USER_HOME=/tmp/bigcasares-gradle bash gradlew clean build
git diff --check
```

Result: build successful; 359 tests passed with zero failures/errors; generated packs
used hash `80e75ffed95a`; `git diff --check` passed.

Live Paper verification used Paper 26.2 build 60 with BetterModel 3.2.0 and a
disposable configuration containing only the resource-pack, item-catalog, and three
item-mechanic modules. The server enabled all five modules. Console verification
confirmed four active definitions at revision `5b8a2c227f8183b6`, an unchanged reload
reported no-op, a temporary display-name edit committed revision `c7f1090f5e2b3e1a`,
an invalid material was rejected while that revision remained active, and restoring the
file committed the original revision again. The server was stopped cleanly.

### Deviations from plan

- Default item definitions are packaged through `src/main/resources`, rather than a
  second Gradle seed archive, because ordinary plugin resource processing already
  preserves the desired JAR/runtime source contract.
- The immutable catalog is Bukkit-free. Bukkit `ItemStack`/PDC mutation is isolated to
  the runtime factory and reconciler because the current Spigot test API has no live
  item factory.
- BigCasares previously loaded at `STARTUP`, before the required BetterModel Paper
  plugin had enabled. The live Paper run exposed this ordering issue, so the descriptor
  now uses `POSTWORLD`; no runtime API or gameplay behavior changed.

### Known limitations

- The console smoke test exercises actual Paper recipe replacement during each catalog
  commit. A connected-player check of item-stack PDC creation, legacy alias upgrade,
  and the batched inventory queue remains a manual acceptance test: the standalone
  Spigot test fixture has no live item factory or player inventory, so the two related
  test-plan substeps remain deliberately unchecked.
- Reconciliation scans online player inventories and ender chests only. Offline player,
  entity, tile, and container scans remain out of scope.
- Item reload validates existing runtime pack assets but does not publish/resend a pack;
  operators run `reload pack` separately after changing assets. Bedrock/Geyser dynamic
  item redefinition remains restart-required.
