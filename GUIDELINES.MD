# Contributor guidelines

The source code, build configuration and tests define current behavior. Start with
[development](docs/development.md), [operations](docs/operations.md), and the
shared [agent instructions](AGENTS.md). Dated plans are historical context.

## Changes and conventions

- Keep changes within the requested scope. Preserve existing gameplay rules,
  stable module/item IDs, persistent-data keys and resource identifiers.
- Platform and core dependency upgrades need explicit approval. Current versions
  are defined in `build.gradle`, the wrapper and `plugin.yml`.
- Use the existing `dev.linqfy.bigCasares` package and neighboring feature package
  names; the repository does not consistently map package names to module IDs.
- Use four spaces, UTF-8, explicit imports, descriptive names, final fields where
  possible, and records for immutable values. Explain non-obvious constraints in
  comments; avoid comments that merely restate code.
- Keep new domain logic testable through small gateways. Existing services vary
  in their use of Bukkit; use runtime/adapters for server effects where practical.
- Follow the surrounding message style: both legacy color strings and Adventure
  components already exist. Log through the plugin logger.

## Modules, content and persistence

- New feature modules implement `PluginModule`, use a stable kebab-case ID, and
  register in `BigCasares.initializeRuntime()` in dependency order. Add an explicit
  `modules.<id>.enabled` default and relevant commands/permissions in `plugin.yml`.
- Prefer `onEnable(RuntimeRegistrationScope)` and `BukkitRuntimeRegistrations`
  for listeners, tasks, commands, recipes and closeables. Ensure partial startup
  and shutdown release owned resources; guard callbacks from retired generations.
- Expose a small public API/gateway when another module needs access. Existing
  bootstrap code wires several cross-module dependencies; don't assume modules
  are completely independent.
- Add settings, services or storage classes only when the feature needs them.
  There is no mandatory seven-file module template.
- Declarative items live in `src/main/resources/content/items/`; follow
  `ItemCatalogModule` and `CustomItemRegistry`, including visual contracts and
  recipes. Some specialized items still use module-specific factories.
- Preserve each feature's existing persistence layout. Data includes per-player
  files, shared YAML stores and plugin-root state files; there is no single
  universal storage path. Never commit live server data or Discord credentials.

## Validation and design records

- Write behavioral tests first for new logic and bug fixes. Mirror the source
  package under `src/test/java`; use JUnit 5 and small fakes/in-memory doubles.
  No mocking framework is configured.
- Test meaningful behavior: validation boundaries, persistence round trips,
  failures, lifecycle cleanup and compatibility. A module-ID assertion alone does
  not establish correct wiring. Documentation-only changes need link/reference
  and command checks, not artificial runtime tests.
- Run focused tests while working, then `bash gradlew --no-daemon clean build`
  before submitting Java, configuration or asset changes. Report the exact
  command, result and any unverified live-server behavior.
- For substantial features, preserve the established dated spec/plan convention
  in `docs/superpowers/`. Record scope, decisions, affected paths and validation;
  retain a concise completion note for work that used a plan. Small fixes and
  documentation edits do not need duplicate specs, plans or agent transcripts.
- Keep current guides updated in the same change. Link to detailed feature docs
  instead of copying them into the automatically loaded agent instructions.

Do not delete uploaded originals, model sources, backups or historical records
without explicit approval. See the [audit](docs/audits/2026-10-14.md) for proposed
cleanup, which is separate from applied documentation edits.
