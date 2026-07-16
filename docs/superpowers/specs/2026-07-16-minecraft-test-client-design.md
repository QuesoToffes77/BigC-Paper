# Minecraft Test Client Design

**Project:** BigCasares
**Date:** 2026-07-16

## Objective

Provide a small, external Java Edition client that joins an isolated Paper server as a
real player so automated checks can inspect custom item stacks and exercise commands
that require a player inventory.

## Scope

The client connects, reports lifecycle/chat/inventory events as JSON lines, and accepts
JSON-line actions from standard input. It supports chat, server commands, movement
controls, look, swing, inventory inspection, waiting for chat, and clean disconnect.

It is a development harness only. It is not packaged into the BigCasares plugin, does
not create or change Minecraft accounts, and does not implement combat, mining,
pathfinding, or public-server automation.

## Confirmed rules

- The default target is `127.0.0.1:25565`; a non-loopback host requires
  `MC_ALLOW_REMOTE=true`.
- The default client identity is `BigCasaresTestBot` using offline authentication,
  suitable only for a local test server configured to permit it.
- Microsoft authentication is opt-in through `MC_AUTH=microsoft`; credentials are
  supplied by Mineflayer's normal device-code flow and are never read from this tool.
- Each input line is one JSON object with an `action` field. Each output line is one
  JSON event object, allowing an agent or a CI script to consume it without terminal
  scraping.
- The Minecraft protocol version is auto-detected unless `MC_VERSION` is explicitly
  set. An unsupported server version must fail with a clear error rather than silently
  using another protocol.

## Architecture

- `lib/config.mjs` reads environment variables, validates the connection target, and
  produces immutable settings.
- `lib/puppet.mjs` owns the Mineflayer bot and maps JSON actions to a constrained
  action set. It emits serializable events through an injected sink.
- `client.mjs` is the process entry point: it connects, reads standard input line by
  line, validates JSON, dispatches actions, and exits cleanly.
- `test/config.test.mjs` covers local defaults, remote-target refusal, explicit remote
  opt-in, and invalid ports without a network connection.

## Configuration

| Variable | Default | Meaning |
| --- | --- | --- |
| `MC_HOST` | `127.0.0.1` | Paper host |
| `MC_PORT` | `25565` | Paper port |
| `MC_USERNAME` | `BigCasaresTestBot` | Bot name or Microsoft account identifier |
| `MC_AUTH` | `offline` | `offline` or `microsoft` |
| `MC_VERSION` | auto | Optional Mineflayer protocol version |
| `MC_ALLOW_REMOTE` | `false` | Required for any non-loopback host |

## Boundaries

- Do not modify Paper, its server properties, the BigCasares plugin, or production
  resource-pack/configuration files.
- Do not add Node/Mineflayer as a Gradle or plugin dependency.
- Do not embed passwords, access tokens, or account credentials.
- Do not permit remote connection by default or add broad gameplay automation.
