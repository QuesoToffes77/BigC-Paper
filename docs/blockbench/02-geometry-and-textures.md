# Geometry and Textures

This section explains how to create the actual 3D model and paint its texture for a BigCasares boss.

## 1. Creating the Geometry

Use a Bedrock Entity export copy for native Bedrock assets. BetterModel's Java source uses a Generic Model `.bbmodel`; keep both copies aligned with the [authoring contract](01-boss-models-overview.md).

1. Open Blockbench and click **Bedrock Entity** under the New menu.
2. Use a descriptive file name and set the exported **Model Identifier** to `geometry.bigcasares.<boss_name>` (e.g., `geometry.bigcasares.abyss_guardian`). Keep it identical to the client entity's geometry reference.
3. **Model Constraints:**
   - Keep the origin `(0, 0, 0)` at the bottom-center of the model. This is where the entity touches the ground.
   - Model in Blockbench units (16 units per Minecraft block); validate dimensions and visibility bounds against the actual gameplay anchor. There is no project-enforced 64-block size allowance.
   - Use standard cubes and clean UV mapping for the shared Java/Bedrock workflow. BetterModel has version-dependent mesh support, but that does not establish native Bedrock export compatibility.
4. **Bone Hierarchy:**
   - You MUST create a root folder/bone named `root`.
   - Place all other bones and cubes inside this `root` bone.
   - `root` is this project's authoring convention, not a Bedrock controller requirement. On Java, BetterModel renders and animates the hierarchy's visual bones.

## 2. Painting the Texture

Once the geometry is complete, it's time to paint the boss.

1. Go to the **Paint** tab in Blockbench.
2. Ensure your UV unwrapping is clean. You can use the "Create Texture" button to generate a template.
3. **Texture Size Recommendations:**
   - A minimum of 64×64 pixels is recommended for standard Minecraft resolution.
   - For highly detailed bosses, use 128×128 or 256×256 pixels, but be mindful of resource pack file size limits.
4. Paint your boss using Blockbench's built-in tools, or export the UV map and paint it in an external editor like Photoshop or Aseprite.

## 3. Exporting the Texture

Export the shared artwork for each renderer:

1. **Bedrock:** Export to `resourcepack/bedrock/textures/boss/<boss_name>.png`
2. **Java:** Embed or resolve the texture in the BetterModel `.bbmodel`, with any external PNG deployed beside it. BetterModel generates its Java textures. The shared registry may also require the matching PNG in `resourcepack/shared/textures/boss/`; this is separate from BetterModel texture resolution.

Files already under `resourcepack/java/assets/bigcasares/textures/boss/` can remain as legacy/shared pack assets; they do not replace BetterModel's generated model textures.

Next, proceed to [Bedrock Animations](03-bedrock-animations.md) to make your boss move.
