---
name: add-new-boss
description: Use when integrating a new PvE boss into the BigCasares framework.
---

# Integrating a New Boss

Follow the repository contributor instructions. Gameplay definitions, BetterModel sources, shared pack assets, and Geyser entity mappings are separate registrations.

## 1. Create the YAML Definition

Create `src/main/resources/bosses/<boss-id>.yml`, using [Abyss Guardian](../../../src/main/resources/bosses/abyss-guardian.yml) or [Sahur](../../../src/main/resources/bosses/tung-tung-sahur.yml) and `AbyssGuardianDefinitionLoader` as schema references.

Set health, audience radius, phase thresholds, UI cadence, `presentation.java-model-key`, animation names, dialogue, and music tracks/fallback. Keep at least one complete ability with targets, telegraph, and effects; use `add-boss-ability` for extensions. `PROJECTILE`, `PULL_CONTINUOUS`, and `SUMMON_CLONE` are implemented; phase 3 reduces cast times/cooldowns. Combos reset eligibility rather than force instant casting.

Use `category-weights`, not `category-cooldowns`. Java plays named `.bbmodel` clips; transform arrays remain legacy metadata. The loader does not read `music.enabled`, `music.radius`, or `music.stop-when-leaving-radius`.

## 2. Register the Boss Definition

Add the ID to `PveBossModule.supportedBossIds()`; `onEnable` loads its definition and runtime. Multiple live instances are supported. Existing `plugins/BigCasares/bosses/` YAML is preserved. Live boss state has no restart recovery.

## 3. Register the Assets

Update `resourcepack/shared/registry.yml` to include the new boss assets:

```yaml
assets:
  <boss-id>:
    type: boss-model
    java-model: bigcasares:<boss_id>
    bedrock-entity: bigcasares:<boss_id>
    texture: boss/<boss_id>
```

For custom music, add a `type: music` entry with matching `java-sound` and `bedrock-sound`, following `abyss-theme`.

## 4. Add Resource Pack Files

Track `.bbmodel` sources under `src/main/resources/bettermodel/models/`, add explicit `BetterModelAssetInstaller` wiring, and deploy under `plugins/BetterModel/models/`. Its filename stem must match `presentation.java-model-key`; the registry's namespaced `java-model` is separate.

Supply native Bedrock entity/geometry/texture/animation/controller files and shared textures/music. Follow [the asset workflow](../../../docs/blockbench/04-java-animations-and-registration.md) for directories and optional offline merging; runtime Java delivery uses a pack stack.

## 5. Geyser Registration

In `GeyserIntegrationModule`, queue a `GeyserCustomEntityDefinition` on the project's `GeyserApiFacade`:

```java
runtime.registerCustomEntity(new GeyserCustomEntityDefinition(
    "WARDEN", "bigcasares:<boss_id>", "<boss_id>_boss_id"));
```

Verify that spawn code assigns this distinct marker to the intended boss and registers it in `ClientEntityPresentationRegistry`. Check owned transient entities separately. Reusing `pve_boss_id` matches Abyss Guardian first; replacing every Warden would affect ordinary mobs. The bridge uses experimental Geyser entity events. Bedrock cast/phase synchronization is not automatic.

## 6. Build and Test

1. Add definition/catalog/asset tests, then run `bash gradlew clean build`.
2. Deploy model sources and update existing runtime YAML/pack sources deliberately; JAR seeds preserve installed files.
3. Start the server and use `/boss spawn <boss-id>` to verify both clients, abilities, multiple instances, and cleanup. Check every requested clip; removal currently requests `death` then closes immediately.
