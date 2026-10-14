# BetterModel 3.2.0 setup for BigCasares

This guide prepares the Java server for BetterModel-rendered Nexus and boss models. BetterModel 3.2.0 is a hard dependency; Geyser's native Bedrock entity mapping is a separate, experimental path.

## 1. Install and verify dependencies

1. Install BetterModel 3.2.0 in the server `plugins/` directory.
2. Install the BigCasares JAR compiled with the BetterModel Bukkit API and with BetterModel declared as a hard dependency.
3. Start the server and confirm that BetterModel enables before BigCasares. If BetterModel is unavailable, BigCasares must fail clearly rather than displaying a flat legacy model.
4. Stop the server once so BetterModel creates its directory structure.

Do not use an unpinned "latest" BetterModel build in production. Keep the server plugin, API dependency, and this documentation on 3.2.0 until the integration is deliberately upgraded and smoke-tested.

## 2. Establish the source directories

Create or deploy these files:

```text
plugins/BetterModel/models/bigcasares_nexus.bbmodel
plugins/BetterModel/models/bigcasares_abyss_guardian.bbmodel
```

`models/` is for entity models that may be attached to a server entity and saved/tracked. Player limb models belong under `plugins/BetterModel/players/`; they are covered in [player animation workflow](07-player-animation-workflow.md).

These Nexus and Abyss Guardian `.bbmodel` files are deployment prerequisites: neither is currently packaged in the repository. Packaged sources live in `src/main/resources/bettermodel/models/`; `BetterModelAssetInstaller` installs Sahur/bat, Ballesta Ark, and toxic mob assets. It replaces differing installed copies, so edit the repository source for those assets.

Use lowercase snake case. The stem is the BetterModel key used by BigCasares:

```text
bigcasares_nexus.bbmodel           -> bigcasares_nexus
bigcasares_abyss_guardian.bbmodel  -> bigcasares_abyss_guardian
```

## 3. Reload and perform a parser smoke test

After every model change:

```text
/bettermodel reload
/bettermodel spawn bigcasares_nexus ghast 1
```

Check texture resolution, root placement, visible bones, and `idle` animation. Delete the temporary model after testing. This command validates BetterModel's model import only; it must never be used as the gameplay spawning mechanism for a Nexus or a boss.

For invisible gameplay anchors, verify `follow-mob-invisibility: false` in BetterModel's installed configuration; otherwise the visual can inherit the anchor's invisibility. See the [official configuration reference](https://github.com/toxicity188/BetterModel/wiki/General-configuration), and confirm behavior on the pinned release.

## 4. Understand tracker ownership

BigCasares wraps BetterModel so gameplay modules use `JavaModelGateway` and `JavaModelHandle` rather than BetterModel classes directly.

```text
authoritative Bukkit entity
  -> JavaModelGateway.attach(anchor, stableModelKey)
  -> BetterModel EntityTracker (one tracker for one anchor)
  -> JavaModelHandle retained by the owning Nexus/boss runtime
  -> JavaModelGateway.animate(handle, animationName)
  -> JavaModelGateway.close(handle) before anchor removal
```

Never attach the same `.bbmodel` twice to the same anchor. Never retain a tracker after its anchor is dead, unloaded, removed, or recovered under a new entity UUID. Closing should be safe to call more than once, because plugin shutdown and normal removal can overlap.

## 5. Publish Java assets

BetterModel emits the resource-pack assets needed to render its model bones. The current `ResourcePackModule` publishes BigCasares and BetterModel as separate entries in a Java pack stack, using `Player.addResourcePack`. It republishes BetterModel output after reload. Make both artifact URLs reachable by clients; `copy-only` publication does not deliver them automatically.

For a separately managed combined ZIP, the offline builder also accepts BetterModel output. Keep its generated namespace separate from `bigcasares` and pass an existing ZIP:

```bash
bash gradlew generateResourcePacks --args="resourcepack build/generated-resourcepacks --bettermodel-java-pack=build/bettermodel/bettermodel-java.zip"
```

Inspect the ZIP before deploying: both `assets/bigcasares/` and BetterModel's asset namespace must exist. Do not merge by overwriting duplicate files; fix the namespace or asset collision.

`-PbettermodelJavaPack` is not consumed by `build.gradle`. The optional CLI merge does not change the runtime pack stack; see [Java animations and registration](04-java-animations-and-registration.md).

## 6. Keep the Bedrock pipeline separate

The BetterModel pack is Java-only. Bedrock has its own source files under `resourcepack/bedrock/`: geometry, textures, animation JSON, controllers, entity definitions, and render controllers. Export both sides from the same Blockbench source, but validate them independently.

Enable `geyser-integration.custom-entities` and `bedrock-resource-pack` to use the native entity path. [Geyser's entity API](https://geysermc.org/wiki/geyser/custom-entities/) requires API 2.11.0 or newer and remains experimental. Existing animation exports do not prove gameplay state is synchronized to Bedrock.

Next: [boss model overview](01-boss-models-overview.md), [placeable Nexus entities](05-placeable-entity-workflow.md), or [boss entities](06-boss-entity-workflow.md).
