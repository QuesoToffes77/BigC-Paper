# Boss models: one source, two client renderers

BigCasares bosses have one authoritative server entity and two client presentations. Model the boss once in Blockbench, then export it for each client pipeline:

| Concern | Java client | Bedrock client through Geyser |
| --- | --- | --- |
| Gameplay authority | Invisible Warden | Same server-side Warden |
| Visual renderer | BetterModel 3.2.0 `EntityTracker` | Native Bedrock client entity |
| Model source | `plugins/BetterModel/models/<model>.bbmodel` | `resourcepack/bedrock/models/entity/bigcasares.geo.json` |
| Animation source | Named Blockbench animations imported by BetterModel | `bigcasares.animation.json` plus a controller |
| Pack delivery | BigCasares and BetterModel in the runtime Java pack stack | BigCasares Bedrock MCPACK |

The runtime owns combat, health, targeting, damage attribution, boss bar, and cleanup around a Warden with vanilla AI disabled. Boss instances are held in memory; persistent entity markers do not provide restart recovery. The tracker owns visual bones and named animations. Never put gameplay logic in an animation or in a Blockbench model.

> Migration note: older BigCasares documentation used a single `ItemDisplay` with server-side transforms. That is legacy guidance. BetterModel renders the Blockbench bone hierarchy itself; do not create a hand-authored Java display model or reproduce its root transforms in YAML.

## Authoring contract

1. Author a **Generic Model** `.bbmodel` for BetterModel, as in the [official authoring guide](https://github.com/toxicity188/BetterModel/wiki/Create-your-own-model-using-BlockBench). Keep an export copy compatible with **Bedrock Entity** for native geometry/animation JSON. Use `bigcasares_abyss_guardian.bbmodel` as the Abyss Guardian Java model key.
2. Make `root` the only top-level bone and put every visible bone below it. Give bones stable, descriptive names such as `body`, `head`, `left_arm`, `right_arm`, and `core`.
3. Keep the ground contact at `(0, 0, 0)`. Test the model at the dimensions of a Warden before adding an offset in code; changing the model origin later breaks both client renderers differently.
4. Use cubes and clean UVs. Avoid hidden decorative bones, duplicate cubes, and empty groups: each meaningful Java bone has a display-packet cost.
5. Name the shared animations `idle`, `cast`, `rage`, `slam`, and `death`. Java uses these exact logical names from the boss definition; Bedrock maps its own fully qualified animation names in the client entity and controller.
6. Export the Bedrock geometry, animation JSON, and controller JSON into `resourcepack/bedrock/`. Do not replace those files with Java-generated output.
7. Put the final Java `.bbmodel` in `plugins/BetterModel/models/`, reload BetterModel, and test it before wiring it to a real boss.

The shipped definitions are [abyss-guardian.yml](../../src/main/resources/bosses/abyss-guardian.yml) and [tung-tung-sahur.yml](../../src/main/resources/bosses/tung-tung-sahur.yml). Abyss Guardian requests `cast` and `slam`; Sahur uses its own `boss_sahur` animation names. Sahur/bat sources are packaged; the Nexus and Abyss Guardian Java models still need manual deployment.

## Java tracker lifecycle

At runtime the Java boundary is `JavaModelGateway`, not the boss module directly:

```text
spawn invisible Warden
        -> models.attach(warden, "bigcasares_abyss_guardian")
        -> BetterModel creates/gets one EntityTracker
        -> models.animate(handle, "idle")
        -> ability/phase changes request named animations
        -> death animation requested (currently closed immediately)
        -> models.close(handle)
        -> remove Warden
```

`JavaModelHandle` is owned by exactly one runtime boss instance. Close it before the Warden is removed and make cleanup idempotent; a despawn event, a shutdown, and an ability completion can race. The model key is the BetterModel filename without `.bbmodel`, not `bigcasares:abyss_guardian` from the legacy shared asset registry.

## Bedrock stays native

Bedrock does not consume BetterModel output. It continues to use:

- [entity definition](../../resourcepack/bedrock/entity/abyss_guardian.entity.json)
- [geometry export](../../resourcepack/bedrock/models/entity/bigcasares.geo.json)
- [animation export](../../resourcepack/bedrock/animations/bigcasares.animation.json)
- [animation controller](../../resourcepack/bedrock/animation_controllers/bigcasares.controller.json)

Keep the geometry identifier (`geometry.bigcasares.abyss_guardian`), texture path, and Bedrock animation names consistent across those assets. Current controllers transition on health queries; `GeyserBossVisualGateway` only stores an in-memory animation name and does not transmit cast/phase properties. `cast`, `slam`, and `death` parity must be implemented and tested separately.

Continue with [Geometry and Textures](02-geometry-and-textures.md), then use [Java animations and registration](04-java-animations-and-registration.md) for the Java-specific import and pack workflow.
