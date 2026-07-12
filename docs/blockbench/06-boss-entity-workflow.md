# Boss entity workflow: Abyss Guardian and future bosses

A BigCasares boss is an authoritative invisible Warden with a BetterModel tracker attached to it. The Warden owns combat and collision. The tracker owns visuals. Keep that division strict.

## Build the source model

1. Start from a Bedrock Entity Blockbench project with `root` as the only top-level bone.
2. Use only useful bones. A visible bone can become a Java display-packet cost, so merge permanently static decoration into nearby geometry instead of creating a bone per fragment.
3. Give combat-relevant visual parts stable names (`head`, `core`, `arm_left`), but do not use them as hitboxes. Server hitboxes remain the Warden and boss ability rules.
4. Create the named animations that gameplay can request: at minimum `idle`, `cast`, `rage`, and `death`; include `slam` where a configured ability requires it.
5. Save as `plugins/BetterModel/models/bigcasares_<boss_id>.bbmodel`, reload BetterModel, and validate with its temporary spawn command.

## Connect the model to gameplay

For `abyss-guardian`, use the stable model key `bigcasares_abyss_guardian`. The runtime sequence is:

```text
/boss spawn abyss-guardian
  -> create invisible Warden and boss health/ability coordinator
  -> attach bigcasares_abyss_guardian tracker
  -> request idle
  -> ability begins: request configured animation (cast or slam)
  -> phase 3: request rage
  -> death: request death, then close tracker and remove Warden
```

An animation request is cosmetic. Do not delay effect application, target validation, cooldowns, damage, drops, or despawn cleanup while waiting for animation playback. If an animation is missing, report the content error and keep the boss gameplay safe; do not leave an invulnerable invisible Warden alive.

## Definition and animation consistency

The source of ability animation names is [abyss-guardian.yml](../../src/main/resources/bosses/abyss-guardian.yml). Before shipping a new model:

1. List every `abilities.*.animation` value.
2. Ensure Blockbench contains an identically named animation.
3. Ensure the Java animation mapper allows the name, or explicitly maps it.
4. Ensure Bedrock exports an equivalent animation and controller state if Bedrock players need the same visual beat.
5. Verify `death` exists even if no ability references it.

## Bedrock assets

Export the same art to the native Bedrock files. Bedrock's client entity, geometry, animation JSON, and controller are independent of BetterModel, as summarized in [boss models overview](01-boss-models-overview.md). Test the Java and Bedrock clients separately; an animation working in one does not prove the other works.

## Performance and failure checklist

- Test a single boss first, then the expected concurrent boss count with nearby Java players.
- Keep looping idle/rage clips simple; expensive animation on many bones is sent repeatedly to viewers.
- Ensure every exit path—death, admin removal, chunk/world unload, plugin disable, and failed spawn—closes the model handle.
- Confirm the Warden cannot visually flash, attack, or expose a vanilla nameplate.
- Confirm actual damage, telegraphs, and targeting originate from the boss runtime rather than from model bones.

For pack delivery use [Java animations and registration](04-java-animations-and-registration.md). For player-specific models, use [player animation workflow](07-player-animation-workflow.md); player animation is deliberately a different, more restrictive system.
