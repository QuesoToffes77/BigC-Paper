# Repository instructions

Scope: this repository. Source and build/configuration files are authoritative;
dated specs/plans are historical context. Keep changes within the user's request.

## Commands

Use JDK 25 in `JAVA_HOME` and `PATH`. The wrapper is not executable in Git.

```bash
bash gradlew test
bash gradlew test --tests 'dev.linqfy.bigCasares.modules.<package>.*'
bash gradlew --no-daemon clean build
bash gradlew generateResourcePacks
bash gradlew runServer
```

Windows: use `gradlew.bat`. `runServer` uses `build/run-server`; install BetterModel
and, for default economy modules, Vault plus an economy provider before testing.
It resolves Paper 26.2 without pinning a server build. Node tooling has separate
checks: `npm --prefix tools/minecraft-test-client ci` then
`npm --prefix tools/minecraft-test-client test` (Node 22+).

## Architecture and constraints

- Paper 26.2 / Java 25 / Gradle 9.2.1; exact dependencies: `build.gradle`.
  BetterModel is required; optional integrations: `src/main/resources/plugin.yml`.
- `BigCasares.initializeRuntime()` creates modules and defines dependency order.
  `ModuleManager` enables in order, rolls back failed startup, disables in reverse.
- New modules implement `PluginModule`; prefer `onEnable(RuntimeRegistrationScope)`
  and `BukkitRuntimeRegistrations` for owned resources and guarded callbacks.
  Global listener/task cleanup is still a fallback, not a completed migration.
- Module switches accept nested `modules.<id>.enabled`, legacy booleans and the
  legacy shop alias; missing switches default to enabled. Add explicit defaults
  for new modules. IDs/config prefixes/package names are not interchangeable.
- Items: `items/catalog/` + `modules/items/`, defaults in
  `src/main/resources/content/items/`. Packs: `modules/resourcepack/`, editable
  sources in `resourcepack/`. BetterModel sources also ship in
  `src/main/resources/bettermodel/models/`; preserve intentional copies/contracts.
- Plugin reload, item reload and pack reload have different scopes and failure
  behavior; see [operations](docs/operations.md). Full reload has no old-runtime
  rollback. A new JAR does not overwrite existing editable pack/item defaults.

## Working rules

- Four spaces, UTF-8, explicit imports; preserve existing package conventions.
  Prefer immutable values and testable gateways; use the plugin logger. Match
  surrounding message APIs (legacy strings and Adventure both exist).
- Test new behavior/fixes with JUnit 5 and small fakes; run focused checks and the
  full build for Java/config/assets. State what was actually verified. For docs,
  check paths, links and commands. Live Paper/Bedrock behavior needs a server.
- Preserve gameplay, stable IDs, persistent keys and user files. Get approval for
  platform/core dependency upgrades and deletions of potentially valuable files.
- Keep credentials, caches and local server data out of Git. Do not blanket-ignore
  `agent_uploads/`: tests read Sahur originals. `tmp/sahur-uv-backup/` is valuable.
- Use specs/plans for substantial features; avoid boilerplate for routine edits.
  Read [contributor guidelines](GUIDELINES.MD) for details and [docs](docs/README.md)
  for feature guides. Load `.agents/skills/` only for the relevant boss task.
