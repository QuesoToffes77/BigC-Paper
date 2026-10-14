# Bedrock Animations

Bedrock Edition plays native animation and controller files on the client. Playback depends on the controller's available state and the installed resource pack.

## 1. Creating Animations

In your Blockbench Bedrock Entity project, switch to the **Animate** tab.

1. **Naming Convention:**
   - Add a new animation and name it `animation.bigcasares.<boss_name>.<anim_name>`.
   - Standard animation names include: `idle`, `cast`, `rage`, `slam`, and `death`.
   - Keep BetterModel's source animation names short (`idle`, `cast`, etc.); apply the Bedrock namespace on the export copy rather than renaming Java's runtime contract.
2. **Keyframing:**
   - Animate the boss by adding Position, Rotation, and Scale keyframes to the `root` bone (and sub-bones).
3. **Loop Modes:**
   - For continuous animations like `idle` or `rage`, set the Loop Mode to **Loop**.
   - For singular actions like `cast`, `slam`, or `death`, set the Loop Mode to **Play Once** (One-Shot).
4. **Exporting:**
   - Go to File > Export > Export Bedrock Animations.
   - Append or save the data into `resourcepack/bedrock/animations/bigcasares.animation.json`.
   - *Note: You may need to manually merge JSON if the file already contains animations for other bosses.*

## 2. Animation Controllers

Animation controllers are state machines whose transitions evaluate Molang expressions. Use only queries or properties actually available to the client; see Microsoft's [controller reference](https://learn.microsoft.com/en-us/minecraft/creator/documents/animations/animationcontroller?view=minecraft-bedrock-stable).

1. Create a controller named `controller.animation.bigcasares.<boss_name>`.
2. Define states like `default` (idle), `casting`, `enraged`, etc.
3. Define transitions from supported queries or explicitly registered/synchronized properties. The current shared controllers use `query.health / query.max_health`; BigCasares does not automatically sync an `is_casting` variable. `GeyserBossVisualGateway` currently stores names only, and the shipped Abyss controller has idle/rage states rather than cast/slam/death transitions.
4. Export or append this controller to `resourcepack/bedrock/animation_controllers/bigcasares.controller.json`.

## 3. Bedrock Entity Definition

Finally, tie the geometry, textures, animations, and controllers together in a client entity definition file.

1. Create a new file at `resourcepack/bedrock/entity/<boss_name>.entity.json`.
2. Link the components:
   ```json
   {
     "format_version": "1.10.0",
     "minecraft:client_entity": {
       "description": {
         "identifier": "bigcasares:<boss_name>",
         "materials": { "default": "entity_alphatest" },
         "textures": {
           "default": "textures/boss/<boss_name>"
         },
         "geometry": {
           "default": "geometry.bigcasares.<boss_name>"
         },
         "animations": {
           "idle": "animation.bigcasares.<boss_name>.idle",
           "cast": "animation.bigcasares.<boss_name>.cast",
           "state": "controller.animation.bigcasares.<boss_name>"
         },
         "scripts": { "animate": [ "state" ] },
         "render_controllers": [ "controller.render.bigcasares.default" ]
       }
     }
   }
   ```

The render controller above is supplied by `resourcepack/bedrock/render_controllers/bigcasares.render.json`. Ensure every animation alias used by controller states appears in the entity definition. Verify actual Geyser client playback; exporting a clip alone does not make it reachable.

Proceed to [Java Animations and Registration](04-java-animations-and-registration.md) to set up the server-side counterpart.
