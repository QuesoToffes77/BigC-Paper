---
name: add-new-boss
description: Use when integrating a new PvE boss into the BigCasares framework. Covers YAML definition, resource pack registration, and testing.
---

# Integrating a New Boss

When adding a new PvE boss to the BigCasares framework, follow these steps to ensure it is properly registered, its abilities are loaded, and its assets are delivered to both Java and Bedrock clients.

For Java clients, the entity visual is a BetterModel source model at `plugins/BetterModel/models/<boss-id>.bbmodel`. The filename is the model key used by the runtime. Keep every animation referenced by the boss YAML in that Blockbench model, generate BetterModel's Java pack, and merge it through the documented `generateResourcePacks -PbettermodelJavaPack=...` workflow.

4. Configure boss behaviours (`abilities`, `animations`, `phases`).
    - Note that you can use advanced effects like `PROJECTILE`, `PULL_CONTINUOUS` (Gravity Well), and `SUMMON_CLONE`.
    - You can also chain abilities using `combo-chance` and `next-combo-ability`.
    - Phase 3 automatically enables Enrage mode (faster cast times and cooldowns).

## 1. Create the YAML Definition

Create a new YAML file in `src/main/resources/bosses/<boss-id>.yml`. Use the following template:

```yaml
id: <boss-id>
display-name: "BOSS NAME"
maximum-health: 10000.0
audience-radius: 64.0

ui:
  update-ticks: 5

phases:
  phase-2-at: 0.66
  phase-3-at: 0.33

category-cooldowns:
  physical: 5
  magic: 8
  movement: 12
  defensive: 15

animations:
  idle:
    type: LOOPING
    duration-ticks: 60
    scale: [1.0, 1.0]
    y-offset: [0.0, 1.0]
    y-rotation: [0, 0]
    priority: 0
  cast:
    type: ONE_SHOT
    duration-ticks: 40
    scale: [1.0, 1.2]
    y-offset: [0.0, 0.5]
    y-rotation: [0, 0]
    priority: 10

abilities:
  # Add abilities here using the add-boss-ability skill

dialogue:
  spawn:
    - "§5The boss appears..."
  phase-2:
    - "§cYou cannot defeat me!"
  defeat:
    - "§7The boss has fallen."

music:
  enabled: true
  radius: 64
  stop-when-leaving-radius: true
  fallback-sound: minecraft:music_disc.5
  tracks:
    phase-1: bigcasares:music.<boss-id>
    phase-2: bigcasares:music.<boss-id>
    phase-3: bigcasares:music.<boss-id>
```

## 2. Register the Boss Definition

In `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java`, update the `onEnable` method to load the new boss definition alongside existing ones. Note: The framework currently supports a single boss instance, but you should structure the loader to support multiple if required.

## 3. Register the Assets

Update `resourcepack/shared/registry.yml` to include the new boss assets:

```yaml
<boss-id>:
  type: boss-model
  java-model: bigcasares:<boss-id>
  bedrock-entity: bigcasares:<boss-id>
  texture: boss/<boss-id>
```

Add the custom music definition if applicable:
```yaml
<boss-id>-theme:
  type: music
  java-sound: bigcasares:music.<boss-id>
  bedrock-sound: bigcasares.music.<boss-id>
```

## 4. Add Resource Pack Files

Ensure the following files are added to the resource pack directory structure:
- **Java model source:** `plugins/BetterModel/models/<boss-id>.bbmodel`. Do not add a hand-authored flat Java entity model under `resourcepack/java/`; BetterModel generates that renderer and its assets.
- **Bedrock Entity:** `resourcepack/bedrock/entity/<boss-id>.entity.json`
- **Bedrock Geometry:** Update `resourcepack/bedrock/models/entity/bigcasares.geo.json`
- **Bedrock Texture:** `resourcepack/bedrock/textures/boss/<boss-id>.png`
- **Bedrock Animations:** Update `resourcepack/bedrock/animations/bigcasares.animation.json`
- **Bedrock Controller:** Update `resourcepack/bedrock/animation_controllers/bigcasares.controller.json`

## 5. Geyser Registration

In `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserIntegrationModule.java`, register the new boss entity mapping:

```java
// WARDEN base entity + marker key -> Bedrock entity identifier
api.registerCustomEntity(
    org.bukkit.entity.EntityType.WARDEN,
    "pve_boss_id", // Assumes same marker key or a specific one for the new boss
    "bigcasares:<boss-id>"
);
```

## 6. Build and Test

1. Run `./gradlew generateResourcePacks` to rebuild the resource packs.
2. Run `./gradlew test` to ensure no tests are broken.
3. Start the server and use `/boss spawn <boss-id>` (if implemented) to verify.
