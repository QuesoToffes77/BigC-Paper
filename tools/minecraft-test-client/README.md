# BigCasares Minecraft Test Client

This untracked local harness joins Paper as a real Java player and exposes a small
JSON-line protocol. It is intended for local verification of player-only flows such as
custom-item PDC, legacy-item upgrades, recipes, and online-inventory reconciliation.

## Setup

```bash
cd tools/minecraft-test-client
npm install
```

The default target is `127.0.0.1:25565`. Remote hosts are refused unless
`MC_ALLOW_REMOTE=true` is set deliberately.

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

No password or token is stored by this tool. Set `MC_VERSION` only when automatic
protocol detection is insufficient.

## JSON-line actions

Send one JSON object per line on standard input. The client writes JSON events to
standard output.

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

## Checks

```bash
npm test
node --check client.mjs
node --check lib/config.mjs
node --check lib/puppet.mjs
```
