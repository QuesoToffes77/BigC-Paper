# Celular Module Design

## Objective

Celular adds a craftable 3D phone that plays short vertical videos on its screen while it is held. Players switch
videos with F (next) and Shift + F (previous), and every village hands out one phone in the first chest somebody opens.
It was built and playtested as a standalone Paper plugin and is integrated into BigCasares as the `celular` module.

## Scope

In scope:
- The Celular item: shaped recipe (8 iron ingots around 1 glass), 3D Java item model with its own texture, and
  `/celular give [jugador]` for admins.
- Videos on the screen as animated item textures (72x128, 10 fps, at most 60 seconds each), one item model per video.
- F / Shift + F video navigation with an action-bar title.
- One phone per village, in the first village chest whose loot is generated.
- Asset generators under `tools/celular/`.

Out of scope:
- Sound for the videos and per-player playback control (see Limits).
- Player-uploaded videos.
- Bedrock visuals and controls (Java item models and animated item textures do not exist on Bedrock; the phone is a
  plain clock there by design).
- Changing BigCasares core dependency versions, other modules or the resource-pack / Geyser module code.

## Confirmed rules

- Recipe: `HHH / HVH / HHH` with H = iron ingot and V = glass; result is one phone (max stack size 1).
- The phone shows the video only in first and third person hands; inventory, ground and item frames show the still
  phone (`minecraft:select` on `display_context`).
- F (swap hands) with the phone in the main hand cancels the swap and moves to the next video; Shift + F moves to the
  previous one. Both wrap around. The item definitions set `hand_animation_on_swap: false`, so the phone does not bob.
- Q is not used on Java: the client removes the item from the hand before the server answers, which plays the
  re-equip animation. Keys such as I, O or G are never sent to the server.
- Bedrock (resolved through `BigCasares#resolvePlayerPlatform`) cannot render the phone, so for Bedrock players it is
  just a clock: crafting the recipe gives a plain vanilla clock plus the chat message
  "Dale bobi, no tenes java?, bancatela pibe", and a phone they get some other way (village chest, a Java friend)
  has no controls and no action bar for them.
- Left-hand display transforms equal the right-hand ones: Minecraft mirrors the left hand itself.
- Every phone remembers its video index in its persistent data; new phones start at the first video.
- Village chests are those whose loot table is `minecraft:chests/village/*`. The first one generated inside a village
  gets a phone added to its normal loot; the village is then flagged in its own `GeneratedStructure` persistent data
  container, so the flag survives restarts.

## Architecture

```
modules/celular/
  CelularModule          PluginModule: settings, videos, recipe, listener and command through BukkitRuntimeRegistrations
  CelularService         navigation (next/previous/clamp), action-bar text and village loot rules; no Bukkit
  CelularSettings        record: recipe.enabled, village-loot.enabled
  CelularSettingsLoader  reads the `celular:` section of config.yml with defaults
  CelularVideo           record: id, title, frames (+ seconds at 10 fps)
  CelularVideosLoader    reads celular/videos.yml from the jar
  CelularRecipe          recipe shape and ingredients as constants
  CelularItems           builds the phone (Paper data components) and reads/writes its video index
  CelularVillages        finds the village around a chest and stores the "already gave" flag
  CelularListener        swap / held / join / loot events, delegates to the service
  CelularCommand         /celular give [jugador]
```

Assets ride the BigCasares Java pack under `resourcepack/java/assets/celular/` (namespace `celular`), like Don Pollos
does with `donpollos`; no extra HTTP server and no `registry.yml` entries (those are for `bigcasares` assets).

- `textures/item/celular.png`: 64x64 atlas for the phone. `models/item/celular.json`: 8 cuboids (body, camera module,
  2 lenses, flash, 3 buttons) with display transforms.
- Per video N: `textures/item/video_N.png` (+ `.mcmeta`, frametime 2), `models/item/celular_video_N.json` (phone + a
  black screen plane + the video plane, emissive) and `items/video_N.json`.

## Config shape

```yaml
modules:
  celular:
    enabled: true

celular:
  recipe:
    enabled: true
  village-loot:
    enabled: true
```

## Limits

- Animated textures loop on the client from the moment the pack loads, so switching videos does not start from the
  beginning and the server cannot sync audio; videos are silent.
- Every frame stays in client memory: about 170 MB raw for the 15 bundled videos (4630 frames), which is why videos are
  10 fps and capped at 60 seconds.
- The Java pack grows by ~28 MB.
- Villages looted before the module was installed already generated their loot and get no phone.
