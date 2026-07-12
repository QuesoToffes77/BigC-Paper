# Boss models: one source, two client renderers

BigCasares bosses have one authoritative server entity and two client presentations. Model the boss once in Blockbench, then export it for each client pipeline:

| Concern | Java client | Bedrock client through Geyser |
| --- | --- | --- |
| Gameplay authority | Invisible Warden | Same server-side Warden |
| Visual renderer | BetterModel 3.2.0 `EntityTracker` | Native Bedrock client entity |
| Model source | `plugins/BetterModel/models/<model>.bbmodel` | `resourcepack/bedrock/models/entity/bigcasares.geo.json` |
| Animation source | Named Blockbench animations imported by BetterModel | `bigcasares.animation.json` plus a controller |
| Pack delivery | BetterModel-generated pack merged with the BigCasares Java pack | BigCasares Bedrock MCPACK |

The Warden owns AI, combat, health, targeting, persistence, damage attribution, boss bar, and cleanup. The tracker owns only the visual bones and their named animations. Never put gameplay logic in an animation or in a Blockbench model.

> Migration note: older BigCasares documentation used a single `ItemDisplay` with server-side transforms. That is legacy guidance. BetterModel renders the Blockbench bone hierarchy itself; do not create a hand-authored Java display model or reproduce its root transforms in YAML.

## Authoring contract

1. Start a **Bedrock Entity** project in Blockbench. Save its source beside the Java BetterModel model while editing; use `bigcasares_abyss_guardian.bbmodel` as the Java model key.
2. Make `root` the only top-level bone and put every visible bone below it. Give bones stable, descriptive names such as `body`, `head`, `left_arm`, `right_arm`, and `core`.
3. Keep the ground contact at `(0, 0, 0)`. Test the model at the dimensions of a Warden before adding an offset in code; changing the model origin later breaks both client renderers differently.
4. Use cubes and clean UVs. Avoid hidden decorative bones, duplicate cubes, and empty groups: each meaningful Java bone has a display-packet cost.
5. Name the shared animations `idle`, `cast`, `rage`, `slam`, and `death`. Java uses these exact logical names from the boss definition; Bedrock maps its own fully qualified animation names in the client entity and controller.
6. Export the Bedrock geometry, animation JSON, and controller JSON into `resourcepack/bedrock/`. Do not replace those files with Java-generated output.
7. Put the final Java `.bbmodel` in `plugins/BetterModel/models/`, reload BetterModel, and test it before wiring it to a real boss.

The current shipped boss is `abyss-guardian`; its gameplay definition is [abyss-guardian.yml](../../src/main/resources/bosses/abyss-guardian.yml). Its abilities already name `cast` and `slam`, so those animation names are part of its public content contract.

## Java tracker lifecycle

At runtime the Java boundary is `JavaModelGateway`, not the boss module directly:

```text
spawn invisible Warden
        -> models.attach(warden, "bigcasares_abyss_guardian")
        -> BetterModel creates/gets one EntityTracker
        -> models.animate(handle, "idle")
        -> ability/phase changes request named animations
        -> death animation finishes
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

Keep the geometry identifier (`geometry.bigcasares.abyss_guardian`), texture path, and Bedrock animation names consistent across those assets. The server's Bedrock notification gateway selects state; it must not be used as the Java rendering path.

Continue with [Geometry and Textures](02-geometry-and-textures.md), then use [Java animations and registration](04-java-animations-and-registration.md) for the Java-specific import and pack workflow.
