# Java animations and BetterModel registration

Java entity models in BigCasares are rendered by BetterModel 3.2.0. BetterModel imports the Blockbench `.bbmodel`, turns its meaningful bones into client display packets, and plays the named animations from that model. BigCasares attaches the model to a real gameplay entity through its `JavaModelGateway`.

> Migration note: a flat Java item model, `ItemDisplay` transform interpolation, and manual scale/translation/rotation keyframes are obsolete for Nexus and boss entity models. Do not add those as a fallback renderer. Projectile visuals may still use ordinary displays when they are not BetterModel entities.

## 1. Put the source model where BetterModel loads it

Use a stable filename; the filename is the Java model key:

```text
plugins/BetterModel/models/bigcasares_nexus.bbmodel
plugins/BetterModel/models/bigcasares_abyss_guardian.bbmodel
```

For a new boss, prefer `bigcasares_<boss_id>.bbmodel` and set `presentation.java-model-key` in its YAML to that stem. Packaged models belong in `src/main/resources/bettermodel/models/` and need explicit installer wiring; simply adding a file there does not deploy it. Nexus and Abyss Guardian currently require manual model deployment.

After copying or changing a model, reload and test the model before changing Java gameplay code:

```text
/bettermodel reload
/bettermodel spawn bigcasares_nexus ghast 1
```

The test spawn proves that BetterModel can parse the model and resolve its texture. Remove the test entity after inspection. It is not a Nexus and it has no BigCasares ownership, persistence, or damage behavior.

## 2. Name animations for gameplay, not for an editor timeline

Put these names in the Blockbench animation list exactly as written:

| Content | Required names | Semantics |
| --- | --- | --- |
| Nexus | `idle`, `damaged`, `critical`, `destroyed` | Looping baseline, one-shot hit, looping low-health state, one-shot teardown |
| Abyss Guardian | `idle`, `cast`, `slam`, `rage`, `death` | Looping baseline, ability one-shots, phase loop, teardown request |

`idle`, `critical`, and `rage` must loop. `damaged`, `destroyed`, `cast`, `slam`, and `death` must be one-shot. Return to the appropriate loop after a one-shot unless the entity is being removed. Keep the Java names deliberately short: the gateway requests `models.animate(handle, "cast")`, while Bedrock uses independently mapped fully qualified animation identifiers.

Loop restoration is an acceptance check, not a guarantee of the current gateway: Nexus and boss callers use `animate`, without a completion callback. Boss removal requests `death` and closes immediately; Nexus removal likewise does not wait for `destroyed` to finish.

For bosses, the `animation` value on an ability in [abyss-guardian.yml](../../src/main/resources/bosses/abyss-guardian.yml) must match a BetterModel animation. Updating animation artwork without keeping this name is a runtime content error.

## 3. Attach through the tracker boundary

The production adapter obtains a BetterModel model renderer by key, creates or retrieves an `EntityTracker` for the Bukkit anchor, and holds it behind `JavaModelHandle`. The expected lifecycle is:

```java
JavaModelHandle model = models.attach(boss, JavaModelKeys.ABYSS_GUARDIAN);
models.animate(model, "idle");

// On cast, phase change, or death:
models.animate(model, animationKey);

// Always before the anchor is removed:
models.close(model);
```

Only the adapter touches BetterModel's Bukkit adapter, tracker creation, and `close()`. Modules retain the handle; they do not cache a BetterModel tracker or manipulate its bones. This makes tracker ownership testable and prevents duplicate trackers on a recovery path.

## 4. Runtime publication and optional offline merge

BigCasares has normal item assets under `resourcepack/java/`; BetterModel generates Java entity-model assets from `.bbmodel` files. `ResourcePackModule` publishes both through a Java pack stack and listens for BetterModel reloads. Editable runtime BigCasares sources are seeded under `plugins/BigCasares/content/pack/`; changing a generated archive is not a source edit.

If deployment requires a combined offline artifact:

1. Generate BetterModel output using the installed configuration; inspect a folder export or ZIP.
2. Keep BetterModel-generated asset paths separate from `bigcasares`. Its upstream external-resource merge option is not required by BigCasares's ZIP merger.
3. Place its generated ZIP at a known path, for example `build/bettermodel/bettermodel-java.zip`.
4. Build BigCasares with the merge input:

   ```bash
   bash gradlew generateResourcePacks --args="resourcepack build/generated-resourcepacks --bettermodel-java-pack=build/bettermodel/bettermodel-java.zip"
   ```

5. Inspect the output ZIP. It must keep `assets/bigcasares/` and add BetterModel's non-conflicting assets. A duplicate non-metadata path is an error to resolve, not a file to overwrite.

The merger keeps BigCasares's `pack.mcmeta` and `pack.png` and rejects other duplicate paths. This optional build does not configure runtime publication. Check the installed 3.2.0 configuration against the [official reference](https://github.com/toxicity188/BetterModel/wiki/General-configuration); verify client acceptance of every delivered pack and avoid asset collisions.

## 5. Verify both clients

1. Start the server with BetterModel enabled before BigCasares.
2. Spawn a Java Nexus and Abyss Guardian. Confirm their loop, one-shot, phase, and death animations.
3. Join via Geyser and confirm the native Bedrock entity is still animated from `resourcepack/bedrock/`, rather than from the Java BetterModel pack.
4. Restart and exercise Nexus recovery. Each surviving Nexus anchor must have one tracker; removed anchors must have none. Boss instances currently live in memory and stop on module disable; persistent Warden markers are not a boss recovery implementation.

For the complete operational flows, see [placeable entities](05-placeable-entity-workflow.md), [boss entities](06-boss-entity-workflow.md), and [player animations](07-player-animation-workflow.md).
