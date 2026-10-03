# Declarative Item Catalog and Reconciliation Design

**Project:** BigCasares
**Phase:** 2 of the Oraxen-inspired content-runtime program
**Date:** 2026-07-15

## Objective

Make the presentation and recipe definitions of BigCasares custom items editable at
runtime without recompiling the plugin, while retaining the existing Java-owned
gameplay mechanics. The first migration covers Copper Apple, Smoke Bomb,
Prismarine Arrow, and Nexus.

## Scope

### In scope

- Seed missing default definitions from the plugin JAR into
  `plugins/BigCasares/content/items/` without overwriting operator edits.
- Parse one YAML definition per item into an immutable candidate catalog.
- Validate unique logical IDs, item models, legacy model data, mechanics, materials,
  recipes, and pack appearance paths before changing the live catalog.
- Use `bigcasares:item_id` as the canonical string PDC identity and
  `bigcasares:item_revision` as the catalog revision marker.
- Read the four existing model-data plus per-item-byte-PDC identities as compatibility
  aliases and upgrade them conservatively when a stack is reconciled.
- Keep Copper Apple, Smoke Bomb, Prismarine Arrow, and Nexus behaviour in their
  existing mechanic classes. Definitions select an allowed mechanic ID; they never
  execute arbitrary YAML actions.
- Build the four owned recipes from catalog definitions and replace them after a
  successful catalog commit.
- Reconcile online player inventories and ender chests in bounded main-thread batches.
- Preserve player-owned mutable state: amount, damage, repair cost, enchantments,
  custom names/lore, and unrelated PDC. Removed or unknown items are left in place
  and marked legacy; they are never destroyed automatically.
- Add `/bigcasares reload items` and item-catalog status output with a dedicated
  permission while retaining the legacy reload permission as a temporary alias.

### Out of scope

- Shared sound definitions or runtime audio changes (Phase 3).
- Nexus/boss presentation reconciliation, BetterModel reload, or Geyser dynamic item
  redefinition (Phase 4).
- Loaded entity, container, tile, or offline-player inventory scans.
- Generic furniture, arbitrary YAML mechanics/actions, custom blocks, glyphs, armor,
  or data-pack reload.
- Automatic pack publication or Java/Bedrock resend. Operators run the existing pack
  reload after changing assets under `content/pack`.

## Confirmed rules

- Runtime `content/items/*.yml` is authoritative after first-run seeding.
- A candidate must be completely parsed and validated before the existing immutable
  catalog is replaced. A failed load leaves the previous catalog, recipes, and item
  behaviour active.
- Logical ID is stable and case-insensitive. Item-model keys and optional legacy
  custom-model-data values are unique across the catalog.
- `bigcasares:item_id` is authoritative when present. Old per-item marker keys and
  model data remain read aliases only; newly created and reconciled stacks use the
  canonical ID.
- Existing mechanics are an allow-list: `copper-apple`, `smoke-bomb`,
  `prismarine-arrow`, and `nexus`. Every definition must select the mechanic expected
  for its logical ID.
- Each definition may declare one shaped recipe. Recipe keys, result amounts, shapes,
  and ingredients are validated before commit.
- Item model references must exist in the runtime Java pack; declared Bedrock textures
  must exist in the runtime Bedrock pack. This phase validates appearance inputs but
  does not introduce a second independent pack publisher.
- Reconciliation runs only on the Bukkit primary thread. It processes a bounded number
  of stacks per tick and stops harmlessly when the module generation retires.

## Catalog format

Each definition is one YAML file under `content/items/`:

```yaml
id: copper_apple
mechanic: copper-apple
material: APPLE
item-model: bigcasares:copper_apple
legacy-custom-model-data: 1001
display:
  translation-key: item.bigcasares.copper_apple
  fallback-name: "Manzana de Cobre"
  lore: []
max-stack-size: 64
components:
  food:
    nutrition: 4
    saturation: 1.2
    can-always-eat: true
recipe:
  key: copper_apple_recipe
  result-amount: 1
  shape: ["CCC", "CAC", "CCC"]
  ingredients:
    C: COPPER_INGOT
    A: APPLE
appearance:
  java-item-definition: java/assets/bigcasares/items/copper_apple.json
  bedrock-texture: textures/item/copper_apple.png
```

`id`, `mechanic`, `material`, `item-model`, display fallback name, and max stack size
are required. Food is optional. The four seeded definitions preserve current gameplay
values and recipes.

## Architecture

### `items.catalog` package

- `CustomItemDefinition` — immutable parsed item metadata, components, recipe, and
  appearance references.
- `CustomItemCatalog` — immutable map keyed by normalized logical ID, with revision
  derived deterministically from normalized definitions.
- `ItemCatalogLoader` — reads YAML files and constructs one candidate catalog.
- `ItemCatalogValidator` — validates definition uniqueness, supported mechanics,
  materials, recipes, and asset paths without modifying Bukkit state.
- `ItemCatalogSeeder` — copies missing default YAML files from the JAR to the runtime
  content folder.
- `CatalogItemStackFactory` — applies canonical identity and catalog-owned appearance
  components to newly created stacks and existing stacks.
- `CatalogRecipeRuntime` — atomically replaces the catalog-owned shaped recipes while
  retaining the old recipes if registration fails.
- `OnlineItemReconciler` — converts compatible online inventory stacks in batches and
  records unchanged, updated, and legacy counts.
- `ItemCatalogReloadResult` — command-facing immutable outcome.

### Existing item API and mechanics

`CustomItemRegistry` holds the current catalog and the existing Java mechanic handlers.
It resolves canonical PDC identity first, then compatibility aliases. The four item
classes become catalog-backed handlers: their mechanics and legacy alias keys remain,
but their item-stack presentation delegates to the catalog factory.

`ItemCatalogModule` loads and commits the catalog before the mechanic modules. It owns
definition seeding, recipes, reconciliation tasks, and reload coordination. Copper
Apple, Smoke Bomb, Custom Crossbow, and Nexus continue to own their listeners/tasks;
they no longer own recipes or presentation prototypes.

## Commit sequence

```text
request -> seed missing defaults -> load candidate -> validate candidate and assets
        -> prepare recipes -> commit catalog -> replace owned recipes
        -> reconcile online stacks in batches -> report counts

candidate failure before commit ------------------------------> keep old catalog
recipe commit failure ----------------------------------------> restore old catalog/recipes
```

The existing pack reload remains separate. If an operator changes pack assets and item
definitions together, they run `reload pack` and `reload items`; the item catalog never
claims that Bedrock/Geyser definitions changed dynamically.

## Configuration

`config.yml` gains only:

```yaml
modules:
  custom-item-catalog:
    enabled: true

custom-item-catalog:
  reconciliation:
    stacks-per-tick: 36
```

`plugin.yml` gains `bigcasares.items.reload` and `bigcasares.items.info` permissions.
The legacy `bigcasares.shop.reload` permission temporarily grants item reload access.

## Recipe extensions (catalog items as ingredients, vanilla results)

Catalog recipes support two extra capabilities used by the gunpowder recipe:

- **Custom item ingredients.** An ingredient value like `bigcasares:potassium_nitrate`
  references another catalog item. The Bukkit gateway registers it as a real
  `RecipeChoice.ExactChoice` over the catalog stack, so identity is the
  persistent-data catalog id: a vanilla item that only shares the material or
  a similar-looking item is never accepted. Vanilla material choices are
  expressed as `COAL|CHARCOAL` and become a `RecipeChoice.MaterialChoice`.
- **Vanilla results.** `result-material: GUNPOWDER` makes the recipe produce a
  vanilla item; the owning definition only hosts the recipe. Without
  `result-material`, the result is the owning custom item as before.

Both are real Bukkit/Paper recipes (`ShapelessRecipe`/`ShapedRecipe` registered
with `Bukkit.addRecipe`), removed and re-registered atomically on
`/bigcasares reload items`, and re-registered on server restart. Ingredients are
consumed by the vanilla crafting flow, so there is no duplication.

## New catalog-only items

The catalog adds two definitions that have no gameplay mechanic module yet and
are reachable through the existing item delivery commands (`/bigcasares give
<id>`, tab completion and `getAllItems()` now resolve catalog-only items through
`CustomItemRegistry`):

- `potassium_nitrate` — Nitrato de Potasio (base `SUGAR`, own model, legacy
  model data 1009). Hosts the shapeless gunpowder recipe
  `potassium_nitrate_gunpowder_recipe`.
- `nitric_acid` — Ácido Nítrico (base `POTION`, own model, legacy model data
  1011). Fully integrated into the catalog and resource pack and documented as
  prepared for future mechanics; it is deliberately not wired into Acid Rain or
  any other gameplay system yet.

Both use the `catalog-material` mechanic allow-listed in `ItemCatalogValidator`
and are wired into the Java/Bedrock resource packs through `shared/registry.yml`
(Java item definitions and models, shared textures, bedrock
`item_texture.json`, and lang entries in `es_es.json`/`en_us.json`).

## Boundaries

- Do not copy Oraxen source, assets, configuration text, API names, or implementation.
- Do not change dependency versions, Java/Paper versions, or existing gameplay balance.
- Do not overwrite runtime item YAML or pack edits when seeding.
- Do not replace the Phase 1 pack transaction, its active manifest, hosting, or
  delivery-state logic.
- Do not delete removed items or scan unloaded/offline storage.
- Preserve unrelated existing dirty Bounty, Nexus, Phase 0, and Phase 1 work.
