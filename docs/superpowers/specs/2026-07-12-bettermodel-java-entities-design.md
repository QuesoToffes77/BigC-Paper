# BetterModel Java Entity Rendering Design

## Goal

Replace BigCasares' flat Java `ItemDisplay` presentation for the Nexus and PvE bosses with BetterModel-rendered Blockbench models and animations, while retaining the existing Bedrock entity resource-pack pipeline.

## Scope

The migration applies to the Java Nexus visual and the Abyss Guardian boss runtime. It does not change combat, persistence, protected containers, team ownership, boss abilities, boss music, Bedrock geometry, or Bedrock animation controllers.

## Architecture

Each feature keeps one authoritative Bukkit entity for game behavior and one BetterModel instance for Java visuals:

- The Nexus keeps its invisible `Ghast` anchor. The BetterModel instance is attached to that anchor; the existing `TextDisplay`, particles, health state, attack state, destruction effects, and persistent data stay in `JavaNexusVisualGateway`.
- The Abyss Guardian keeps its invisible `Warden` for targeting, damage, ability effects, and health. The BetterModel instance follows that Warden; the existing `TextDisplay`, boss bar, music, and ability coordinator remain unchanged.
- BetterModel becomes a required runtime dependency. Module startup must fail with a clear, actionable log message when BetterModel is unavailable. BigCasares must not silently fall back to the current flat models.

## Model and Animation Contract

Java models are authored once in Blockbench and imported into BetterModel. Bedrock continues to consume the existing `.geo.json`, animation, controller, and entity-definition files.

The shared animation identifiers are:

- Nexus: `idle`, `damaged`, `critical`, `destroyed`.
- Abyss Guardian: `idle`, `cast`, `rage`, `death`.

The Java runtime triggers those names through an adapter instead of mutating `ItemDisplay` scale, transform, or position directly. BetterModel owns bone transforms and interpolation. Gameplay code only requests semantic animation states.

## Integration Boundary

Introduce a small internal Java model gateway rather than exposing BetterModel types through Nexus and boss domain services. The adapter owns model creation, attachment, animation playback, visibility, and removal. The Nexus and boss modules depend only on this gateway.

Model identifiers remain stable:

- `bigcasares:nexus`
- `bigcasares:abyss_guardian`

The exact BetterModel import format and model IDs must be verified against the selected compatible BetterModel release before wiring the Gradle dependency.

## Lifecycle and Failure Handling

- Spawn: create the gameplay entity, apply the BetterModel instance, then start `idle`.
- Update: trigger semantic animations from Nexus health/attack transitions and boss ability/phase transitions.
- Removal: remove the BetterModel instance before removing its gameplay anchor.
- Recovery: Nexus visual recovery reattaches the model when reconstructing saved Nexus records.
- Missing or invalid Java model: log the model ID and fail the affected spawn without leaving orphan gameplay or model entities.

## Verification

Tests cover the internal gateway contract and ensure that Nexus and boss lifecycles request model creation, the expected animation identifiers, and removal. Existing resource-pack validation remains green. Runtime verification runs a Paper server with BetterModel installed, spawns a Nexus and Abyss Guardian, and confirms that their Blockbench models and animations render for Java clients. Bedrock regression verification confirms the generated `.mcpack` still includes the existing native assets.
