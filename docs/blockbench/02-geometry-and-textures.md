# Geometry and Textures

This section explains how to create the actual 3D model and paint its texture for a BigCasares boss.

## 1. Creating the Geometry

To ensure compatibility with Bedrock edition (and because it allows for bone hierarchy), we start our project as a Bedrock Entity.

1. Open Blockbench and click **Bedrock Entity** under the New menu.
2. Set the **File Name** and **Model Identifier** to `geometry.bigcasares.<boss_name>` (e.g., `geometry.bigcasares.abyss_guardian`).
3. **Model Constraints:**
   - Keep the origin `(0, 0, 0)` at the bottom-center of the model. This is where the entity touches the ground.
   - Recommended maximum size is 64×64×64 blocks. Anything larger may cause rendering issues or clipping.
   - Use standard cubes and per-face UV mapping. Do not use complex meshes, as they are not supported natively by Minecraft entity renderers.
4. **Bone Hierarchy:**
   - You MUST create a root folder/bone named `root`.
   - Place all other bones and cubes inside this `root` bone.
   - The `root` bone is required for Bedrock animation controllers and keeps the BetterModel bone hierarchy organized. On Java, BetterModel's tracker renders and animates every named bone in that hierarchy.

## 2. Painting the Texture

Once the geometry is complete, it's time to paint the boss.

1. Go to the **Paint** tab in Blockbench.
2. Ensure your UV unwrapping is clean. You can use the "Create Texture" button to generate a template.
3. **Texture Size Recommendations:**
   - A minimum of 64×64 pixels is recommended for standard Minecraft resolution.
   - For highly detailed bosses, use 128×128 or 256×256 pixels, but be mindful of resource pack file size limits.
4. Paint your boss using Blockbench's built-in tools, or export the UV map and paint it in an external editor like Photoshop or Aseprite.

## 3. Exporting the Texture

Because both Java and Bedrock use the same texture file, you need to export the PNG to two locations in the project structure:

1. **Bedrock:** Export to `resourcepack/bedrock/textures/boss/<boss_name>.png`
2. **Java:** Export to `resourcepack/java/assets/bigcasares/textures/boss/<boss_name>.png`

Next, proceed to [Bedrock Animations](03-bedrock-animations.md) to make your boss move.
