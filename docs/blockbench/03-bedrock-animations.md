# Bedrock Animations

Bedrock Edition handles animations entirely client-side using native animation and animation controller files. This allows for smooth, lag-free animations.

## 1. Creating Animations

In your Blockbench Bedrock Entity project, switch to the **Animate** tab.

1. **Naming Convention:**
   - Add a new animation and name it `animation.bigcasares.<boss_name>.<anim_name>`.
   - Standard animation names include: `idle`, `cast`, `rage`, `slam`, and `death`.
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

Animation controllers act as state machines, telling the Bedrock client when to play which animation based on variables sent from the server.

1. Create a controller named `controller.animation.bigcasares.<boss_name>`.
2. Define states like `default` (idle), `casting`, `enraged`, etc.
3. Set up transitions based on Molang queries (e.g., `query.is_casting`). The BigCasares plugin syncs these variables automatically when abilities are used.
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
           "cast": "animation.bigcasares.<boss_name>.cast"
         },
         "animation_controllers": [
           { "anim": "controller.animation.bigcasares.<boss_name>" }
         ],
         "render_controllers": [ "controller.render.default" ]
       }
     }
   }
   ```

Proceed to [Java Animations and Registration](04-java-animations-and-registration.md) to set up the server-side counterpart.
