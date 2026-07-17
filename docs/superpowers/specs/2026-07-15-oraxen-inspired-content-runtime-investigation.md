# Oraxen-Inspired Content Runtime Investigation

**Date:** 2026-07-15
**BigCasares baseline:** Paper 1.21.11, Java 21
**Oraxen baseline:** local `oraxen/` checkout, version 1.217.0, commit `8cb371ac2f5521fcb47c2cf3772dc089a24210e4` (2026-06-23)
**Status:** investigation and approval proposal only; no production implementation is authorized by this document

## Executive conclusion

BigCasares should adopt Oraxen's operational ideas, not transplant Oraxen's implementation.

The highest-value outcome is a transactional BigCasares content runtime with:

- scoped reload commands;
- immutable item, sound, entity-presentation, and pack snapshots;
- asynchronous build into a staging directory;
- validation before any live state changes;
- atomic publish and snapshot swap;
- content-derived pack identity and versioned URLs;
- explicit listener, task, recipe, external-subscription, and entity ownership;
- rollback to the previous working snapshot on every failure;
- reconciliation of existing items and presentations after a successful commit.

Oraxen's operator experience is strong, but its current reload is a sequential mutation of global state rather than a transaction. Copying it literally would import the same partial-reload and lifecycle risks. BigCasares already has stronger domain boundaries, deterministic pack output, Java/Bedrock separation, BetterModel-backed animated entities, and focused tests. Those strengths should remain the foundation.

The first implementation must fix BigCasares's existing full-reload lifecycle. Several modules register listeners that are not retained or unregistered in `onDisable()`, so repeated `/bigcasares reload` calls can leave old listeners active. Resource-pack hot reload should not be layered on top of that behavior.

## Legal boundary

Oraxen is source-available under a custom license, not a permissive open-source license. Its `LICENSE.md` says full, partial, and modified redistribution is not allowed without the copyright holder's permission. BigCasares must therefore use a clean-room behavioral implementation:

- It is safe to study documented behavior and architecture.
- Do not copy Oraxen classes, methods, configuration text, assets, or substantial code fragments.
- Do not preserve Oraxen-specific names or APIs in BigCasares.
- If BigCasares will ever be distributed, obtain written permission before incorporating any Oraxen code.
- The proposed design below is original to BigCasares and deliberately uses its existing gateways, modules, records, and test style.

Primary source: [Oraxen license at the reviewed commit](https://github.com/oraxen/oraxen/blob/8cb371ac2f5521fcb47c2cf3772dc089a24210e4/LICENSE.md).

## Sources reviewed

### BigCasares

- `BigCasares#reloadPluginState`, runtime initialization, and module order
- `ModuleManager` and every module's `onDisable()` implementation
- the complete `modules/resourcepack` package and its tests
- `CustomItem`, `CustomItemRegistry`, all four registered custom item implementations, and item consumers
- BetterModel gateway and Nexus/PvE boss presentation lifecycles
- Geyser resource-pack, custom-item, custom-entity, and subscription bridge
- Java and Bedrock pack sources, shared registry, sounds, models, and manifests
- boss music runtime and pack-loaded gating
- relevant design and Blockbench documentation

### Oraxen

- plugin startup/shutdown and manager replacement
- reload command and every reload scope
- item parsing, item registry, item updater, templates, components, and model-data allocation
- mechanic factory registration, listener ownership, task ownership, and unloading
- pack generation, validation, merging, atlas generation, obfuscation, multi-version generation, upload, self-hosting, dispatch, and status handling
- sound parsing, `sounds.json` generation, custom playback, and jukebox datapack generation
- furniture factory, display/interaction entities, PDC persistence, updater, orphan cleanup, storage, and text display recovery
- pack and item API events
- reload-related history and test coverage

Official behavior references:

- [Oraxen reload commands](https://docs.oraxen.com/usage/commands)
- [resource-pack generation options](https://docs.oraxen.com/configuration/advanced-pack-generation)
- [pack hosting](https://docs.oraxen.com/plugin-setup/pack-hosting)
- [pack merging](https://docs.oraxen.com/plugin-setup/pack-merging)
- [custom items](https://docs.oraxen.com/creating-content/items)
- [sound configuration](https://docs.oraxen.com/configuration/sound-yml)
- [furniture](https://docs.oraxen.com/creating-content/furniture)
- [custom block choices](https://docs.oraxen.com/creating-content/blocks)

## System comparison

| Area | Oraxen 1.217.0 | BigCasares now | Recommended direction |
|---|---|---|---|
| Authoring | Runtime YAML folders for items, sounds, glyphs, mechanics, recipes, and pack files | Items are Java classes; assets live in repository folders; boss definitions have YAML | Add a BigCasares content directory for declarative definitions while keeping gameplay mechanics in Java |
| Pack generation | Runtime generation, merging, validation, atlas/shader/font/model generation, compression, upload | Deterministic build-time Java and Bedrock ZIPs with manifest/checksums | Reuse deterministic builders at runtime behind a staged pipeline; add validation and publishing |
| Reload | Granular commands plus `all`; sequential global mutations; pack generation partly async | Full disable/recreate of every module | Granular transactional reload; do not recreate unrelated gameplay modules for pack/item changes |
| Item identity | One PDC string ID plus generated appearance metadata | Per-item PDC byte key plus global numeric model-data lookup | One stable `bigcasares:item_id` string; `item_model` as primary appearance; legacy model data only as compatibility metadata |
| Existing item updates | Player, ender chest, loaded entity, and tile inventories can be reconciled | No reconciliation after definition changes | Add a conservative, batched item reconciler with an explicit preservation policy |
| Sounds | YAML-to-`sounds.json`, namespaces, variants, jukebox metadata/datapack | One hand-written Java and Bedrock boss track definition | One shared logical sound catalog generating both editions; keep playback behind gateways |
| Entities | General furniture through ItemDisplay/Interaction/ItemFrame/ArmorStand; optional ModelEngine | Purpose-built BetterModel entities for Nexus and bosses; native Geyser definitions | Keep BetterModel architecture; adopt definition snapshots, recovery, and orphan cleanup patterns only |
| Bedrock | Primarily Java resource-pack/content system | First-class Bedrock pack, custom item/entity definitions, forms, and fallbacks | Preserve BigCasares's advantage; make dynamic Geyser limitations explicit |
| Extensibility | Static API, Bukkit lifecycle events, mechanic factories, pack mutation event | Internal modules and narrow gateways | Add typed internal lifecycle events/results; avoid mutable pack lists exposed to arbitrary listeners |
| Failure policy | Many entries fail softly; reload can partially mutate state | Build validation fails fast; runtime module enable logs and continues | Prepare everything first, commit once, retain previous snapshot on failure |
| Testing/design | Broad feature suite and years of production fixes; global/static design is hard to isolate | Strong domain-service tests and deterministic pack tests | Retain BigCasares testing style and add reload transaction/integration coverage |

## How Oraxen's hot reload actually works

`/oraxen reload all` currently performs these operations in order:

1. Unregister tracked mechanic listeners.
2. Cancel tracked mechanic tasks and clear selected runtime caches.
3. Reload messages/config manager.
4. Reload paintings, including another config reload.
5. Recreate native mechanic factories from the new mechanics configuration.
6. Reload items into static registries and fire `OraxenItemsLoadedEvent`.
7. Optionally scan/update online inventories, entity inventories, tile inventories, and placed furniture.
8. Recreate font and sound managers and start pack generation.
9. Reload HUD state, including another config reload, event re-registration, and task restart.
10. Reload recipes.
11. Refresh glyph tab completion for online players.

Single-pack generation uses a useful phased async pipeline: item assets on a pack worker, Bukkit-sensitive work on the main thread, file collection and post-processing on the worker, final event work on the main thread, ZIP writing on the worker, then upload/dispatch. An atomic flag rejects overlapping generation requests. Upload tracks URL and SHA-1 and resends only when the published content changed.

This produces a good operator experience, but it is not atomic:

- success messages are sent before the requested operation completes;
- a second pack generation request is silently skipped while the command still appears successful;
- configs are replaced several times during `all`;
- item/model-data allocators are cleared before item parsing succeeds;
- exceptions can leave old registries paired with partially reset supporting state;
- the live pack file is deleted before a replacement is completely generated;
- single-pack generation is asynchronous, but the multi-version branch performs substantial generation synchronously;
- managers and registries are mutable statics/singletons;
- generated/uploaded/local live state is not committed as one version;
- datapack-backed features still require a restart in some cases;
- furniture type/config migrations have documented limits for already placed entities.

The Oraxen Git history contains many fixes specifically for reload listener leaks, duplicate mechanics, stale upload managers, pack send ordering, self-hosting, changed hashes, furniture races, shader state, and glyph allocation. That history is valuable evidence: reload is a lifecycle protocol, not just reparsing YAML.

## Oraxen strengths worth adopting

### Operator workflow

- Granular scopes: items, pack, recipes, configs, messages, HUDs, paintings, and all.
- Direct pack send/retry commands.
- Reload-time update options for existing items and placed content.
- Automatic upload/self-hosting and resend on content changes.
- Clear content folders that allow rapid iteration without rebuilding the plugin JAR.

### Pack pipeline

- A build pipeline with identifiable phases and main-thread boundaries.
- Generation serialization to prevent concurrent writers.
- Direct assets plus ordered imported pack layers.
- Structured generation for models, modern item definitions, sounds, fonts, atlases, and metadata.
- Validation of resource paths, JSON, and image readability before distribution.
- SHA-based change detection.
- Hosting abstraction and pack-dispatch abstraction.
- Client-version filtering and optional pre-join delivery.
- Events around generation and upload.

### Runtime reconciliation

- Existing item updates are treated as part of content reload, not ignored.
- Player inventories are accessed on the appropriate scheduler/region context.
- Large entity and chunk scans are batched.
- Placed furniture is identified by persistent logical IDs and can be recovered or removed when its definition disappears.

### Mechanics lifecycle

- Mechanic listeners and tasks are registered through an owner that can unregister them by mechanic ID.
- Native factories are replaced while external factories can remain registered.
- Long-lived caches with reload implications are explicitly cleared.

### Content model

- One logical item definition joins item metadata, appearance, and mechanics.
- Stable PDC item IDs decouple identity from appearance allocation.
- Sounds are logical definitions rather than hand-maintained JSON only.
- Modern `item_model`/item definitions are supported in addition to older custom model data.

## Oraxen weaknesses not to migrate

### Global mutable state

Static item maps, model-data maps, mechanic maps, pack output maps, and a global plugin singleton make ordering important and rollback difficult. BigCasares should use immutable snapshot objects and constructor-injected services.

### Partial reload semantics

Oraxen mutates each subsystem immediately. If a later phase fails, earlier phases stay changed. BigCasares should parse, validate, build, and publish a candidate before swapping any live reference.

### In-place live file replacement

Deleting or overwriting the currently served pack creates a window where downloads can fail or see inconsistent bytes. BigCasares should write a content-addressed artifact and atomically switch the active manifest only after success.

### Silent concurrency behavior

Skipping a duplicate request is reasonable, but the caller needs a structured `already-running`, `coalesced`, or `queued` result. Operators should never receive a success message for work that did not run.

### Broad configuration mutation

Oraxen automatically migrates and writes configuration in several loaders. This is convenient but risky during reload. BigCasares should make migrations explicit, backed up, versioned, and separate from ordinary validation where practical.

### Complexity outside BigCasares's needs

Oraxen supports old server/client versions, several custom-block carriers, shader armor, glyph shaders, pack obfuscation/protection, numerous generic mechanics, and multiple third-party item systems. Porting those features would increase maintenance without helping the current 1.21.11 game design.

### Generic mechanics mixed with domain gameplay

BigCasares's smoke bomb, Nexus, crossbow, boss, shop, and team systems have domain services and tests. Replacing them with a generic YAML action engine would weaken invariants and debugging. Configuration should select and parameterize Java mechanics, not replace the domain layer.

### Furniture is not a general custom-entity renderer

Oraxen's entity-heavy subsystem is primarily placeable furniture. It uses display/interaction entities and optionally ModelEngine. It is not a replacement for BigCasares's BetterModel-backed combat entities, authoritative anchors, boss state, or native Bedrock entity path.

### Licensing

The custom Oraxen license is itself a reason to avoid source transplantation. Clean-room behavior is both technically and legally safer.

## BigCasares strengths to preserve

### Deterministic, strict pack construction

`DeterministicZipWriter` sorts entries and zeroes timestamps, producing stable bytes. `MergedJavaResourcePackBuilder` rejects conflicting non-metadata paths instead of silently applying last-writer-wins. These behaviors are safer than Oraxen's permissive imported-pack override model and should remain the default.

### First-class Java and Bedrock outputs

BigCasares validates and builds separate Java and Bedrock artifacts from a shared registry. Oraxen does not offer an equivalent first-class native Geyser content pipeline. This is a strategic BigCasares advantage.

### Purpose-built entity architecture

The invisible authoritative Bukkit entity plus `JavaModelGateway` plus native Bedrock definition is the correct split for the Nexus and Abyss Guardian. Gameplay remains server authoritative while visuals are replaceable adapters.

### Narrow gateways and testable domain services

Resource-pack delivery, client platform detection, model rendering, boss audio, economy, persistence, and UI already have useful boundaries. A content runtime can fit behind these interfaces instead of introducing Oraxen-style static APIs.

### Content hash and pack status foundations

BigCasares already computes deterministic hashes, derives a Java pack UUID, filters foreign pack status events, and tracks player pack state. Those are good primitives that need to be lifted into a versioned live snapshot.

### Existing test health

The targeted resource-pack, Geyser, BetterModel, and reload-command suite passed 35 tests during this investigation. The gap is not basic correctness; it is live lifecycle and transaction coverage.

## BigCasares gaps and current risks

### P0: current reload can duplicate listeners

`BigCasares#reloadPluginState()` disables active modules, reloads config, and constructs an entirely new runtime. Bukkit listeners registered by old modules remain active unless explicitly unregistered.

Examples found in the current tree:

- `CopperAppleModule` registers an anonymous craft listener and does not unregister it.
- `SmokeBombModule` registers three listeners; shutdown stops projectile work but does not unregister the listener objects.
- `CustomCrossbowModule` registers six listeners and does not unregister them.
- `MissionModule`, `BountyModule`, and `SkillRatingModule` register listeners while their `onDisable()` methods do not unregister them.
- Other modules correctly retain and unregister listeners, demonstrating inconsistent ownership rather than an intentional plugin-wide cleanup strategy.

This must be corrected before expanding reload.

### Runtime pack is immutable in practice

- Pack generation happens in Gradle and is bundled into the JAR.
- `ResourcePackModule` reads the bundled manifest first.
- Reload reconstructs delivery state but does not rebuild assets.
- `copy-only` is the default publishing mode.
- `embedded-http` exists in the enum/config parser but cannot produce a Java pack URI and has no server implementation.
- There is no upload, versioned local hosting, or atomic active-manifest switch.

### Final Java identity excludes BetterModel input

The manifest version is derived from the BigCasares resource tree before BetterModel is merged. The merge updates Java SHA-1 but retains the prior version and UUID. A BetterModel-only change can therefore produce different final bytes under the same pack UUID/version. The final pack identity must be derived from the final merged archive, not only the BigCasares input tree.

### Validation is existence-oriented

The current validator checks registered files exist, but does not comprehensively validate:

- JSON syntax and expected object shapes;
- PNG readability and path casing;
- model-to-texture references;
- individual sound file references and OGG presence;
- Java item-definition/model consistency;
- Bedrock JSON relationships and format versions;
- duplicate archive paths across all layers;
- `pack.mcmeta` compatibility;
- unused/legacy root assets affecting the tree digest.

### Item definitions are code-only and identity is split

- Four custom items build Java prototypes in separate classes.
- Names, lore, components, item models, numeric model data, and marker keys are duplicated across Java, resource files, Geyser definitions, and language files.
- Identity lookup starts from global numeric model data, then verifies a per-item PDC byte key.
- Existing items are never reconciled after content changes.
- Numeric model data is globally unique even though modern item models no longer require that as the primary identity.

### Sound definitions are manual and edition-specific

The Abyss Guardian track is hand-written in both Java `sounds.json` and Bedrock `sound_definitions.json`. The runtime track knows playback keys, but there is no shared sound catalog, schema validation, or pack generation from logical definitions.

### Pack status is tied to one service instance

A reload discards all player states and recognizes only the current manifest UUID. A real pack swap needs a transition window where status events for both old and new requests can be interpreted correctly, and it needs per-player last-requested/last-loaded pack identity rather than one enum without a version.

### Geyser definitions are lifecycle registrations

The bridge collects definitions and registers them when Geyser fires its define-resource-packs/items/entities lifecycle events. Recreating the bridge after those events have already fired does not itself prove that Geyser will redefine live content. Closing the old bridge during BigCasares reload may therefore remove the only active event subscriber without dynamically updating Geyser's already-built registries.

Until a supported dynamic Geyser redefinition API is verified, Bedrock pack/item/entity changes must report `restart-required` or use an explicitly tested Geyser reload integration. Java hot reload must not pretend Bedrock was updated.

### No reload job model

There is no reload ID, scope, progress, duration, changed/no-op result, warning list, failure phase, rollback result, or `status` command. The current command reports success after a synchronous full restart attempt regardless of subsystem-level outcomes logged by `ModuleManager`.

## Proposed BigCasares architecture

### Immutable content snapshot

Create one immutable `ContentSnapshot` containing at least:

- snapshot ID and creation time;
- source digest;
- `CustomItemCatalog`;
- `CustomSoundCatalog`;
- `EntityPresentationCatalog`;
- recipe definitions owned by content;
- final Java artifact metadata: path, SHA-1/SHA-256, content-derived UUID, URL;
- final Bedrock artifact metadata: path, SHA-256, manifest UUID/version;
- warnings and compatibility requirements;
- BetterModel source/output revision metadata when present.

The live runtime holds an `AtomicReference<ContentSnapshot>`. Readers always see one complete version.

### Reload coordinator

Introduce a single `ContentReloadCoordinator` with these phases:

```text
request -> acquire/coalesce -> discover -> parse -> validate -> build in staging
        -> validate artifacts -> publish candidate -> commit snapshot on main thread
        -> reconcile runtime -> optionally dispatch -> report result

any pre-commit failure ---------------------------------------> keep old snapshot
any post-publish/pre-commit failure -> retire unpublished candidate -> keep old snapshot
```

Required behavior:

- only one content build writes at a time;
- a repeated equivalent request is coalesced or returns `already-running`;
- work that can run off-thread does so;
- Bukkit registry/listener/entity mutations run on the correct server thread;
- the previous snapshot remains active until commit;
- an unchanged final artifact is a successful no-op and is not resent;
- results are structured, logged once, and returned to the command sender after completion;
- staging directories are unique per job and cleaned safely;
- cancellation during plugin disable cannot commit afterward.

### Runtime ownership scope

Add a `RuntimeRegistrationScope` (or equivalent) owned by every module/runtime generation. It tracks:

- Bukkit listeners;
- scheduled tasks;
- recipes/keys;
- command executor bindings where replacement matters;
- external event-bus subscriptions;
- closeable gateways/workers;
- transient entities or presentation handles when the module owns their lifetime.

Closing the scope is idempotent and releases resources in reverse registration order. Module tests should verify close behavior. A plugin-wide listener/task cleanup can be used as a temporary safety net during migration, but explicit ownership is the target.

### Content source layout

Recommended runtime layout:

```text
plugins/BigCasares/content/
  items/*.yml
  sounds.yml
  entities/*.yml
  recipes/*.yml
  pack/
    java/...
    bedrock/...
    shared/registry.yml
    imports/...
```

Repository `resourcepack/` remains the default source. Gradle packages it as seed content. On first run, BigCasares extracts missing defaults into the data folder; afterward the data folder is runtime-authoritative. Defaults must not overwrite operator edits.

### Staged pack build and publish

Retain the deterministic builders but change their runtime contract:

1. Read one candidate content snapshot.
2. Build Java and Bedrock packs in `packs/.staging/<job-id>/`.
3. Merge BetterModel output through an explicit `JavaPackLayerProvider`.
4. Apply ordered layers with a declared conflict policy.
5. Validate the complete archives.
6. Derive version and UUID from final artifact bytes.
7. Publish under a content-addressed name such as `bigcasares-java-<sha256-prefix>.zip`.
8. Atomically replace a small active manifest, never the bytes currently being served.
9. Retain the previous artifact for a grace period so in-flight client downloads finish.

Default conflict behavior should remain `FAIL`. Optional per-layer or per-path overrides can be added explicitly. Structural mergers may be provided for `sounds.json`, language JSON, font providers, atlases, and compatible metadata; arbitrary last-writer-wins should not be the default.

### Publishing

Recommended first publisher is an embedded, versioned HTTP server because BigCasares already declares the mode and the desired Oraxen-like workflow is zero-touch iteration.

Requirements:

- bind address, public base URL, port, and optional trusted proxy settings are explicit;
- immutable hash-addressed URLs;
- correct content length/type and cache headers;
- bounded worker pool;
- streaming or zero-copy file response rather than reading the whole pack for every request;
- clean shutdown and port-release tests;
- old artifacts served during a configurable grace window;
- no directory listing or arbitrary path access;
- external URL/copy-only remains supported for production CDN workflows.

### Items

Introduce immutable `CustomItemDefinition` values for stable content fields:

- logical ID;
- base material;
- item model key;
- display translation key/name;
- lore translation keys/text;
- stack size and vanilla components;
- optional legacy custom model data;
- mechanic IDs plus validated mechanic settings;
- update policy metadata.

Use one PDC string key, `bigcasares:item_id`, as identity. Preserve the current per-item markers as read aliases during migration so existing items continue to resolve. `minecraft:item_model` remains the primary 1.21.11 appearance. Numeric custom model data becomes optional compatibility information, not registry identity.

Gameplay remains Java-owned. For example, `mechanics: [smoke-bomb]` selects the existing tested smoke-bomb mechanic; it does not execute arbitrary YAML actions.

After commit, an item reconciler can update existing stacks in bounded batches. Recommended default policy:

- preserve amount, damage, repair cost, enchantments, player rename, and mutable runtime PDC;
- refresh item model, translation-based default name, default lore only when not player-modified, vanilla components owned by the definition, and definition revision;
- never delete unknown/removed items automatically; mark them legacy and report counts;
- scan online inventories first; loaded entity/tile inventories are a later opt-in due to cost.

### Sounds

Create one logical sound schema that generates both Java and Bedrock definitions. Initial fields:

- logical ID and Java/Bedrock event keys;
- category;
- one or more files;
- stream/preload;
- volume, pitch, weight, and attenuation where supported;
- subtitle/translation key;
- replacement behavior;
- optional duration/range metadata.

`CustomSoundCatalog` validates referenced OGG files before pack commit. Runtime code requests logical IDs through `SoundPlaybackGateway`; platform adapters resolve edition-specific keys. Existing boss music tracking remains valuable and should stop obsolete keys during a successful sound-definition transition.

Jukebox datapacks should not be in the first migration. Datapack registry mutation and client pack reload are different lifecycles, and even Oraxen reports restart requirements for generated datapacks in some cases.

### Entities and placed presentations

Do not replace BetterModel with Oraxen furniture.

Add declarative `EntityPresentationDefinition` values only for visual/runtime mapping:

- logical presentation ID;
- authoritative Java anchor type;
- required persistent marker(s);
- BetterModel model key and named animation set;
- Bedrock entity identifier;
- recovery/update policy;
- optional fallback presentation.

The Nexus and boss modules continue to own gameplay, persistence, damage, and spawning. The model gateway should gain snapshot/revision awareness and `closeAll()` ownership. Existing anchors should be reconciled after a compatible model-definition commit; incompatible anchor or persistence changes return `restart-required` or require an explicit migration.

Adopt Oraxen's useful placed-content ideas:

- stable logical ID in PDC;
- base/interaction relationship metadata;
- chunk/entity-load recovery;
- orphan detection when definitions disappear;
- explicit cleanup tools;
- batched reconciliation.

Do not adopt generic seats, storage, barriers, custom-block carriers, or evolution until BigCasares has a concrete gameplay need for them.

### Geyser/Bedrock policy

Separate artifact generation from live application capability:

- `JAVA_APPLIED`
- `BEDROCK_APPLIED`
- `BEDROCK_RESTART_REQUIRED`
- `BETTERMODEL_RELOAD_REQUIRED`

Preserve the existing Geyser bridge across ordinary Java/config reloads instead of closing and recreating it blindly. Its live snapshot references may be replaceable, but any lifecycle-registered resource pack/item/entity definitions must only be reported as updated after an integration test proves the installed Geyser API applies them dynamically.

### Commands and observability

Recommended command surface:

```text
/bigcasares reload config
/bigcasares reload content
/bigcasares reload items
/bigcasares reload sounds
/bigcasares reload pack
/bigcasares reload all
/bigcasares reload status [job-id]
/bigcasares pack send <player|all>
/bigcasares pack info
```

Every result should include job ID, scope, duration, changed/no-op, old/new snapshot ID, Java/Bedrock application status, number of reconciled items/entities, warnings, and failure phase. Permission should no longer be named `bigcasares.shop.reload`; introduce content/config-specific permissions and retain the old permission as a temporary alias.

## Migration phases

### Phase 0 — reload safety and lifecycle ownership (mandatory)

Goal: make the existing reload repeatable before adding new content behavior.

- Inventory all listener/task/recipe/external-subscription ownership.
- Introduce the runtime registration scope.
- Convert modules that leak anonymous listeners/tasks.
- Make module disable idempotent.
- Add repeated reload tests and a reload result model.
- Stop reporting unconditional success when individual module enable fails.
- Keep the current pack build/delivery behavior unchanged.

Exit criteria:

- ten repeated reload cycles produce exactly one active listener/task/recipe set;
- disabling during an in-progress task prevents later callbacks from mutating the new runtime;
- existing gameplay module tests remain green.

### Phase 1 — transactional Java pack hot reload

Goal: edit assets in the data folder, run one command, and safely deliver a new Java pack.

- Seed runtime content files.
- Add coordinator, staging, enhanced validator, final-byte identity, and active manifest.
- Implement versioned embedded HTTP plus existing external/copy-only modes.
- Preserve strict merge behavior and add BetterModel as an explicit layer provider.
- Add `reload pack`, `pack info`, `pack send`, and structured completion output.
- Track old/new pack status during transition.

Exit criteria:

- valid asset change builds, publishes, swaps, and resends once;
- no-op does not resend;
- invalid JSON/PNG/reference leaves the old pack and URL active;
- BetterModel-only byte changes produce a new UUID and URL;
- concurrent reload requests cannot corrupt output or claim false success;
- a download already in progress survives the swap.

### Phase 2 — declarative item catalog and reconciliation

Goal: change stable item presentation/components without rebuilding the plugin.

- Add item definitions, parser, validation, immutable registry, and single PDC identity.
- Migrate the current four items through compatibility aliases.
- Keep complex behavior in existing modules/mechanic handlers.
- Generate/validate item appearance assets.
- Rebuild owned recipes after commit.
- Add conservative online-inventory reconciliation.

Exit criteria:

- old and new item stacks resolve to the same logical ID;
- duplicate IDs/models and invalid mechanics reject the candidate;
- failed item load retains the entire old catalog;
- player-owned mutable state survives reconciliation;
- removed definitions do not silently destroy items.

### Phase 3 — shared sounds and runtime audio transition

Goal: define a sound once and generate/use it for Java and Bedrock.

- Add shared sound schema and catalog.
- Generate Java and Bedrock sound JSON.
- Validate OGG references and identifiers.
- Route boss music through logical sound IDs.
- Stop obsolete active sound keys during commit.

Exit criteria:

- one definition produces both edition outputs;
- invalid/missing audio rolls back;
- sound-only changes produce a new pack identity and correct runtime key;
- current boss fallback rules remain green.

### Phase 4 — entity presentation reconciliation and platform capability

Goal: reload compatible visual mappings without changing gameplay ownership.

- Add presentation definitions and snapshot/revision handling.
- Add BetterModel coordination contract.
- Reconcile loaded Nexus/boss presentations safely.
- Verify and encode Geyser dynamic capabilities.
- Add restart-required reporting for unsupported transitions.

Exit criteria:

- every tracker has one owner and closes once;
- compatible presentation changes do not duplicate anchors/trackers;
- incompatible changes are refused or explicitly migrated;
- Java/Bedrock result reporting is truthful.

### Phase 5 — optional Oraxen-like conveniences (separate approvals)

Do not include these in the initial handoff:

- generic furniture;
- custom NoteBlock/StringBlock/ChorusBlock/ShapedBlock systems;
- generic YAML action mechanics;
- glyph/HUD shader generation;
- custom armor generation;
- pack obfuscation/protection;
- multi-version pack fan-out;
- automatic third-party configuration mutation;
- jukebox/datapack hot reload.

Each is a standalone product feature with its own design, performance budget, persistence model, and tests.

## Recommended decisions for approval

1. **Approve Phases 0–3 as the initial program.** Phase 4 begins only after the pack/items/sounds transaction is stable. Phase 5 remains out of scope.
2. **Approve a clean-room implementation.** No Oraxen code or assets are copied.
3. **Approve `plugins/BigCasares/content` as runtime-authoritative after first-run seeding.** Repository `resourcepack/` remains the packaged default source.
4. **Approve content-addressed embedded HTTP as the default development publisher**, while retaining external URL/copy-only for production.
5. **Approve strict merge conflicts by default.** Overrides require explicit layer/path configuration.
6. **Approve one string PDC item ID with compatibility reads for existing markers.** Do not delete the old markers immediately.
7. **Approve conservative online-inventory reconciliation.** Loaded entity/tile scans are opt-in and deferred until batching is proven.
8. **Approve Java-first live application.** Bedrock changes report restart-required until the installed Geyser API is proven dynamically reloadable.
9. **Do not automatically execute `/bettermodel reload` in the first implementation.** Treat BetterModel output as an explicit pack layer and report when its own reload is required.
10. **Rename reload permissions by subsystem**, retaining `bigcasares.shop.reload` only as a transitional alias.

## Handoff brief for the implementing agent

This brief becomes actionable only after the decisions above are approved.

### First handoff scope

Implement Phase 0 only. Do not start runtime pack generation in the same change.

The agent must first create the required approved design spec and checkbox implementation plan under `docs/superpowers/`, following `GUIDELINES.MD`. The plan must preserve the unrelated bounty and Nexus changes already in the working tree.

### Phase 0 likely touch points

- `BigCasares.java`: replace unconditional full reload response with a structured coordinator/result boundary.
- `PluginModule`: either expose runtime ownership or keep the interface stable and inject a scope into modules.
- `ModuleManager`: return enable/disable outcomes and prevent partial success from being reported as complete.
- new `modules/reload` or `module/runtime` package: registration scope, reload result, phase/status types.
- modules that currently leak listeners/tasks/recipes: retain registrations through the scope.
- `BigCasaresCommand`: scoped permissions and asynchronous/final completion messaging where needed.
- tests mirroring every new service/type; no Mockito.

### Phase 0 non-goals

- no item YAML format;
- no pack HTTP server;
- no resource-pack source relocation;
- no BetterModel/Geyser reload behavior changes beyond preventing lifecycle regression;
- no generic mechanics/furniture/blocks;
- no changes to Minecraft, Java, Paper/Spigot, BetterModel, or Geyser versions.

### Required tests

- registration scope closes listeners, tasks, recipes, and closeables once in reverse order;
- close is idempotent;
- a failed module enable returns a failed reload result;
- old runtime callbacks cannot mutate a new generation;
- repeated reload orchestration has no duplicate owned registrations;
- command output reflects success, no-op, already-running, and failure accurately;
- existing targeted pack/Geyser/BetterModel tests remain green;
- full test suite passes.

### Verification commands

```bash
bash gradlew test
git diff --check
git status --short
```

Use a writable `GRADLE_USER_HOME` if required by the environment, and do not commit Gradle cache files.

## Investigation verification

Command used for the targeted baseline:

```bash
bash gradlew test \
  --tests 'dev.linqfy.bigCasares.modules.resourcepack.*' \
  --tests 'dev.linqfy.bigCasares.items.*' \
  --tests 'dev.linqfy.bigCasares.modules.model.*' \
  --tests 'dev.linqfy.bigCasares.modules.geyser.*' \
  --tests 'dev.linqfy.bigCasares.command.BigCasaresCommandTest'
```

Result: `BUILD SUCCESSFUL`; 35 executed test cases across the matching existing classes. No production code was changed by this investigation.
