# Server Governance and Discord Design

## Objective

Provide operator-only server controls, visible moderation telemetry without automatic punishment, and an optional Discord administration/chat integration. Minecraft-facing text and public Discord presentation are Spanish.

## Scope

The server-control module owns persistent PvP and gameplay rules, resistance, the operator GUI, and session-only vanish. The moderation module owns curated audit events, rolling abuse scores, observations, redaction, and staff notifications. The Discord module owns JDA lifecycle, managed messages, remote administration, private log delivery, and both chat bridge directions. A shared exact-token emoji service is used by chat and the bridge.

Covert resource-pack probing, automatic punishment, device fingerprinting, NMS, ProtocolLib, and an API for unrelated plugins are out of scope.

## Confirmed Rules

- `/servercontrol` and `/control` require an online player whose `isOp()` value is true.
- A timed PvP override keeps the original persistent baseline until it expires or a permanent selection replaces it.
- End access and elytra rockets are denied to ordinary players while disabled; vanished operators bypass both.
- Global resistance is a hidden minimum. Stronger external resistance remains authoritative until it expires.
- Vanish hides the player entity and tab-list entry from all viewers, blocks revealing gameplay interactions and ordinary chat, and ends on disconnect.
- Abuse signals contribute to a 60-second score. Warning and critical alerts have cooldowns and never punish automatically.
- Discord is optional. Invalid or absent secrets disable only Discord.
- Every Bukkit operation initiated by Discord is scheduled on the primary server thread.
- Only the genuine configured resource pack may produce the documented low-confidence cache-timing signal.

## Architecture

- `servercontrol`: `ServerControlService` is the pure state machine; `YamlControlStorage` persists state; Bukkit controllers implement GUI, rules, resistance, and vanish.
- `moderation`: `AuditService` publishes redacted structured events; `AbuseScoringService` maintains rolling scores/cooldowns; listeners collect curated signals and observations.
- `discord`: `DiscordGateway` isolates transport; `JdaDiscordGateway` owns JDA; command, embed, bridge, queue, and validation helpers remain independently testable.
- `communication`: `EmojiAliasService` performs a single non-recursive token pass.

## Config Shape

`modules.server-control-system.enabled`, `modules.moderation-system.enabled`, and `modules.discord-integration.enabled` gate modules. Settings live below matching top-level keys. Runtime control state lives in `data/server-control-system/state.yml`, observations under `data/moderation-system`, managed Discord IDs in `data/discord-integration/state.yml`, and token/webhook secrets in `discord-secrets.yml`.

## Boundaries

The modules do not change existing gameplay modules, expose public services to third-party plugins, inspect packets, make punishment decisions, or request additional resource packs.
