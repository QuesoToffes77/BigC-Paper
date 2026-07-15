# Server Governance, Moderation, and Discord Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Implement the server-control, moderation, and optional Discord modules described in the companion design, including shared emoji aliases and durable/testable boundaries.

## Tech Stack

- Java 25 toolchain
- Paper API 26.2 build 56 alpha
- JDA 6.4.1
- Gradle and JUnit 5.11.4

## File Structure

- `modules/servercontrol`: persistent controls, GUI, vanish, resistance, and gameplay listeners.
- `modules/moderation`: audit contracts, scorer, observations, collectors, and redaction.
- `modules/discord`: gateway, JDA runtime, embeds, commands, logs, secrets, and bridge.
- `communication/EmojiAliasService.java`: shared exact-token replacement.
- `config.yml`, `plugin.yml`, `build.gradle`, `BigCasares.java`: configuration and wiring.

## Boundaries

Do not add automated punishment, packet interception, covert pack probes, device identifiers, NMS, ProtocolLib, or unrelated gameplay changes.

## Tasks

1. Shared contracts and state domain
   - [x] Add emoji alias replacement and tests.
   - [x] Add control records, storage, transition service, recovery, and tests.
   - [x] Add audit records, redaction, rolling scorer, and tests.
   - Verification: focused domain tests pass without a running server.

2. Server-control runtime
   - [x] Add operator command and both inventories.
   - [x] Apply PvP to current/new worlds and recover/expire overrides.
   - [x] Enforce End, rockets, resistance, and session vanish.
   - [x] Audit actions and feed restriction signals.
   - Verification: wiring and state tests pass; Paper API compilation succeeds.

3. Moderation runtime
   - [x] Collect curated lifecycle/chat/command/login/movement/combat/inventory/pack signals.
   - [x] Store privacy-sensitive observations for 30 days and purge daily.
   - [x] Notify opted-in operators and publish alert events without punishment.
   - Verification: thresholds, expiry, cooldowns, redaction, and persistence tests pass.

4. Discord runtime
   - [x] Generate protected secrets file and fail independently on invalid configuration.
   - [x] Maintain two public Spanish messages and private severity logs.
   - [x] Add authorized field editing and confirmed console execution.
   - [x] Add loop-safe, mention-safe, emoji-aware bidirectional bridge.
   - Verification: gateway helpers and validation tests pass; JDA compilation succeeds.

5. Integration
   - [x] Register modules and commands in stable order.
   - [x] Add defaults and runtime libraries.
   - [x] Run `./gradlew clean build`.
   - Verification: complete build succeeds.

---

## Agent Completion Report

**Agent:** Codex (GPT-5)
**Date completed:** 2026-07-15
**Branch:** main

### What was built

- Build and bootstrap: `build.gradle`, `settings.gradle`, `src/main/resources/plugin.yml`, `src/main/resources/config.yml`, and `src/main/java/dev/linqfy/bigCasares/BigCasares.java`.
- Design trail: `docs/superpowers/specs/2026-07-15-server-governance-design.md` and this plan.
- Shared communication: `EmojiAliasService.java`.
- Server control: `ControlMenuHolder.java`, `ControlState.java`, `ControlStorage.java`, `PvpTransition.java`, `ResistanceLevel.java`, `ResistanceManager.java`, `ServerControlCommand.java`, `ServerControlListener.java`, `ServerControlMenu.java`, `ServerControlModule.java`, `ServerControlService.java`, `ServerControlSettings.java`, `TimedPvpOverride.java`, `VanishManager.java`, `VanishSessionRegistry.java`, and `YamlControlStorage.java`.
- Moderation: `AbuseScoreResult.java`, `AbuseScoringService.java`, `AbuseSeverity.java`, `AbuseSignal.java`, `AuditEvent.java`, `AuditRedactor.java`, `AuditService.java`, `AuditSeverity.java`, `AuditSink.java`, `FileObservationStorage.java`, `ModerationListener.java`, `ModerationLogHandler.java`, `ModerationModule.java`, `ModerationSettings.java`, `ModerationSignalTracker.java`, `MovementTracker.java`, `Observation.java`, `ObservationStorage.java`, and `RareItemGainTracker.java`.
- Discord: `DiscordEmbedFactory.java`, `DiscordFieldValidator.java`, `DiscordGateway.java`, `DiscordIntegrationModule.java`, `DiscordMessageState.java`, `DiscordSecrets.java`, `DiscordSettings.java`, `JdaDiscordGateway.java`, `RemoteCommandPolicy.java`, and `YamlDiscordStateStorage.java`.

### Tests written

- `EmojiAliasServiceTest`: defaults, unknown aliases, exact tokens, and non-recursive replacement.
- `ServerControlServiceTest`: timed/permanent transitions, replacement, expiry recovery, resistance cycling, and staff preferences.
- `YamlControlStorageTest`: complete persisted control-state round trip.
- `ServerControlModuleWiringTest`: stable module ID.
- `AbuseScoringServiceTest`: rolling expiry, warning/critical thresholds, cooldowns, and no punishment.
- `AuditRedactorTest`: command credential, token, and webhook redaction.
- `FileObservationStorageTest`: raw private observation retention, first-seen state, and purge.
- `RareItemGainTrackerTest`: unexplained versus observed rare-item gains.
- `ModerationModuleWiringTest`: stable module ID.
- `DiscordFieldValidatorTest`: preset fields, quotes, colors, URLs, ports, and limits.
- `RemoteCommandPolicyTest`: dangerous classification and user-bound expiring confirmations.
- `YamlDiscordStateStorageTest`: both managed-message IDs.
- `DiscordModuleWiringTest`: stable module ID.

### Testing instructions

Run all related tests with:

```
bash gradlew test --tests "dev.linqfy.bigCasares.communication.*" --tests "dev.linqfy.bigCasares.modules.servercontrol.*" --tests "dev.linqfy.bigCasares.modules.moderation.*" --tests "dev.linqfy.bigCasares.modules.discord.*"
```

Expected result: all tests pass, no failures.

### Deviations from plan

- Java remains at the repository's existing Java 25 target; the Foojay resolver was added so the declared toolchain can be provisioned reproducibly from a Java 21 host.
- Console capture includes logger output and the dispatch result. Bukkit commands that write only to the native console sender may not expose every output line through the API.
- Manual two-client and Discord-guild verification requires operator credentials and a live Paper server and was not automated.

### Known limitations

- Packet/authentication security scoring can only consume warnings surfaced by the server logger; it does not inspect packets and intentionally adds no ProtocolLib or NMS dependency.
- If Discord rejects the privileged message-content intent, the bot reconnects without it and clearly disables only inbound Discord chat.
