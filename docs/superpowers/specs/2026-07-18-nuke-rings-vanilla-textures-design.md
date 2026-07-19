# Nuke Rings and Vanilla Textures Design

## Goal

Make the Nuke Shot visibly expand from one compressed TNT into real horizontal TNT rings. The completed formation contains exactly 200 TNT with populations that grow approximately exponentially. The Nuke Shot and Tracker Compass reuse Minecraft's built-in TNT and compass appearances without downloading or adding replacement textures.

## Nuke Formation

The formation contains one center point plus five rings. Ring populations are fixed at `6`, `13`, `26`, `51`, and `103`, for exactly 200 TNT including the center. Ring radii remain evenly spaced by the configured `radius-step`, whose default is 10 blocks. The default radii are therefore `10`, `20`, `30`, `40`, and `50` blocks. Points within each ring are evenly distributed by angle.

The center TNT appears immediately at the configured height above the activating player. One complete ring appears every configured `ring-interval-ticks`. Previously displayed TNT remains visible, so the formation grows outward from the compressed center instead of replacing it. After the fifth ring appears, all 200 displays are atomically replaced with primed TNT. The center participates in the explosion.

The population curve is normalized doubling rather than literal powers of two so the total remains exactly 200. The outer rings intentionally become much denser than circumference-proportional rings to preserve the requested exponential visual.

## Runtime and Failure Handling

`NukeRingLayout` remains the Bukkit-free geometry boundary and returns the five rings. `NukeAnimationRuntime` owns the center point separately, then owns display creation, ring timing, conversion to primed TNT, and cleanup.

If the world disappears, display creation fails, or primed-TNT creation fails, the runtime removes entities it created using the existing cleanup behavior. Existing TNT fuse, explosion, damage, protection, and source attribution rules remain unchanged.

## Vanilla Item Appearance

The Java resource-pack model for Nuke Shot references Minecraft's vanilla TNT block model or texture through a local model definition. The Tracker Compass references Minecraft's vanilla compass item model. Neither item uses a new or downloaded PNG.

Compatibility copies under both Java resource-pack trees stay aligned. Catalog identity, custom item IDs, interaction mechanics, names, lore, and legacy model data remain unchanged. Existing custom PNG files may remain unused unless removing them is demonstrably safe for every pack assembly path.

## Configuration

The nuke default total becomes 200. The default ring count becomes five because the center is not a ring. The default `radius-step` becomes `10.0`; existing height and ring-interval defaults remain unchanged. Configuration validation requires a positive total, ring count, and radius step and rejects combinations that cannot produce the normalized exponential layout.

## Verification

Test-first coverage will establish:

- one center plus ring populations `6, 13, 26, 51, 103`;
- exactly 200 unique formation points;
- evenly spaced radii and angular distribution;
- default ring radii of `10`, `20`, `30`, `40`, and `50` blocks;
- the center remains present through expansion and is included at release;
- five ring steps at five-tick intervals;
- default settings load 200 TNT and five rings;
- Nuke Shot and Tracker Compass model JSON references use vanilla Minecraft assets.

Verification runs the focused special-items tests first, followed by the full Gradle test and build tasks. `gradle clean` is excluded so `build/run-server` remains intact.

## Boundaries

- Do not download or generate textures.
- Do not change tracker behavior beyond its item appearance.
- Do not change global TNT behavior or protection systems.
- Do not modify dependency, Minecraft, Java, or Gradle versions.
- Preserve unrelated work in the dirty worktree.
