# Minecraft Test Client Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

## Goal

Create a small external Mineflayer client for local BigCasares/Paper verification. It
must represent a real player, accept a constrained JSON-line action protocol, and make
unsafe remote targets opt-in.

## Tech stack

- Node.js 26 (locally available)
- Mineflayer 4.37.1
- Node built-in test runner

## File structure

- `tools/minecraft-test-client/package.json` — isolated dependency and scripts.
- `tools/minecraft-test-client/client.mjs` — JSON-line process entry point.
- `tools/minecraft-test-client/lib/config.mjs` — immutable environment settings.
- `tools/minecraft-test-client/lib/puppet.mjs` — constrained Mineflayer action bridge.
- `tools/minecraft-test-client/test/config.test.mjs` — no-network configuration tests.
- `tools/minecraft-test-client/README.md` — setup, local-server prerequisites, and
  action protocol examples.

## Boundaries

- No plugin/Gradle dependency or runtime changes.
- No account credentials, remote defaults, pathfinding, combat, or mining automation.
- Preserve existing dirty worktree changes.

## Tasks

1. Define and test connection settings
   - [x] Add failing tests for local defaults, unsafe remote hosts, remote opt-in, and
     invalid ports.
   - [x] Implement the immutable settings parser.
   - Verification: tests run without Mineflayer or a network connection.

2. Add the constrained client bridge
   - [x] Add the package manifest with pinned Mineflayer and no global installation.
   - [x] Implement lifecycle/chat/inventory event emission and supported actions.
   - [x] Implement JSON-line standard-input dispatch and clean shutdown.
   - Verification: syntax checks and configuration tests pass.

3. Document and verify
   - [x] Document installation, safe local-server configuration, authentication, and
     JSON-line examples.
   - [x] Install dependencies and run the Node test suite.
   - [x] Run a no-server configuration smoke check and `git diff --check`.
   - Verification: the harness is runnable without changing the plugin or server.

---

## Agent Completion Report

**Agent:** Codex (GPT-5)
**Date completed:** 2026-07-16
**Branch:** `main`

### What was built

- Added the untracked `tools/minecraft-test-client` Node harness, including a pinned
  Mineflayer dependency and ignored local dependency/auth-cache directories.
- Added loopback-only-by-default settings with offline/cracked authentication as the
  default and a deliberate remote-target opt-in.
- Added JSON-line lifecycle, chat, inventory, movement, look, command, wait, and
  disconnect operations.
- Added setup and JSON-line protocol documentation.

### Tests written

- `test/config.test.mjs` — covers safe defaults, explicit remote opt-in, port
  validation, and supported authentication values without opening a socket.
- `test/puppet.test.mjs` — uses a fake bot to cover connection settings, command and
  movement dispatch, inventory events, rejected actions, and cleanup after connection
  failure.

### Testing instructions

```bash
cd tools/minecraft-test-client
npm test
node --check client.mjs
```

Expected result: two test files pass with zero failures. `npm install` completed using
Mineflayer 4.37.1; the Node test suite and syntax/import checks passed.

### Deviations from plan

- The client remains intentionally untracked and is not registered in Gradle or the
  plugin. Its local `node_modules` and auth-cache files are ignored.
- The default is offline/cracked authentication as requested; Microsoft authentication
  remains available only as an explicit environment option for a future test server.

### Known limitations

- An offline/cracked bot needs an isolated Paper server configured with
  `online-mode=false` and `enforce-secure-profile=false`; do not use those settings in
  production.
- The current sandbox rejects loopback sockets, so a real server connection was not
  attempted here. The next Paper run can drive the bot through its JSON-line standard
  input.
- Mineflayer auto-detects the protocol by default. If its installed version does not
  support a future Paper protocol, connection fails visibly rather than selecting a
  different version.
