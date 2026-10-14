# BigCasares Minecraft Test Client

This local harness joins Paper as a real Java player and exposes a small
JSON-line protocol. It is intended for local verification of player-only flows such as
custom-item PDC, legacy-item upgrades, recipes, and online-inventory reconciliation.

## Setup

Use Node.js 22 or newer (required by the pinned Mineflayer 4.37.1 dependency).

```bash
cd tools/minecraft-test-client
npm ci
```

The default target is `127.0.0.1:25565`. The configured host guard accepts
`localhost`, `::1`, and strings starting with `127.`; other hosts require
`MC_ALLOW_REMOTE=true`. This is a hostname check, not DNS/address validation.

For a local server in offline mode:

```bash
MC_AUTH=offline MC_USERNAME=BigCasaresTestBot npm start
```

That requires an isolated server configured with `online-mode=false` and
`enforce-secure-profile=false`. Do not apply those settings to a production server.

For a normal local Paper server, Mineflayer can use its Microsoft device-code flow:

```bash
MC_AUTH=microsoft MC_USERNAME=your-email@example.com npm start
```

The wrapper does not request a password. Mineflayer's Microsoft flow caches
authentication tokens in the user's `.minecraft` directory by default; this
wrapper does not override `profilesFolder`. See [Mineflayer's authentication
documentation](https://github.com/PrismarineJS/mineflayer#usage). Set `MC_VERSION`
only when automatic protocol detection is insufficient.

Settings: `MC_HOST`, `MC_PORT`, `MC_AUTH` (`offline` by default), `MC_USERNAME`,
`MC_VERSION`, and `MC_ALLOW_REMOTE` (`true` or `false`).

## JSON-line actions

Send one JSON object per line on standard input. The client writes JSON events to
standard output.

Wait for the `ready` event before issuing actions. Actions execute sequentially;
`waitForChat` matches a substring in future messages only, so a reply that arrives
before that action starts is not matched. Use the emitted `chat` events when
checking immediate command replies. End standard input after `quit` to finish the
process.

```json
{"action":"command","command":"bigcasares items info"}
{"action":"command","command":"bigcasares give copper_apple"}
{"action":"inventory"}
{"action":"waitForChat","pattern":"Catálogo BigCasares","timeoutMs":5000}
{"action":"setControlState","control":"forward","value":true}
{"action":"setControlState","control":"forward","value":false}
{"action":"position"}
{"action":"quit"}
```

Supported actions are `chat`, `command`, `setControlState`, `look`, `swingArm`,
`inventory`, `position`, `waitForChat`, and `quit`.

This client checks protocol-visible behavior. It has no graphical renderer or
recipe/crafting action and cannot validate BetterModel visuals or Bedrock clients.

## Checks

```bash
npm test
node --check client.mjs
node --check lib/config.mjs
node --check lib/puppet.mjs
```
