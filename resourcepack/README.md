# BigCasares Resource Pack

This folder is intentionally separate from the plugin source code.

## Structure

- `pack.mcmeta`
- `assets/bigcasares/items/copper_apple.json`
- `assets/bigcasares/models/item/copper_apple.json`
- `assets/bigcasares/textures/item/copper_apple.png`
- `assets/bigcasares/lang/en_us.json`
- `assets/bigcasares/lang/es_es.json`

## Build zip

1. Open this `resourcepack` folder.
2. Zip the contents of this folder (not the folder itself).
3. Host the zip and set it in `server.properties` with `resource-pack` and `resource-pack-sha1`.

## Notes

- Minecraft `1.21.11` uses resource pack format `75`.
- The plugin sets the item's `item_model` to `bigcasares:copper_apple`, so the pack no longer depends on overriding vanilla `minecraft:apple`.
- The plugin still writes `CustomModelData: 1001` for server-side custom item identification.
