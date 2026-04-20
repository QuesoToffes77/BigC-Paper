# BigCasares Contributor Guidelines

> These rules apply to all contributors — human and agentic alike.  
> When an AI agent builds a module, it must follow every section of this document.

---

## Table of Contents

1. [Stack & Versions](#1-stack--versions)
2. [Project Layout](#2-project-layout)
3. [Module System](#3-module-system)
4. [Code Style](#4-code-style)
5. [Test-Driven Development](#5-test-driven-development)
6. [Superpowers Directory (Agents)](#6-superpowers-directory-agents)
7. [Agent Tagging Requirements](#7-agent-tagging-requirements)
8. [Build & Run](#8-build--run)
9. [Config & Resources](#9-config--resources)
10. [Do & Do Not](#10-do--do-not)

---

## 1. Stack & Versions

| Concern | Value |
|---|---|
| Minecraft | 1.21.11 Paper (Spigot architecture) |
| API | `org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT` |
| Economy | `com.github.MilkBowl:VaultAPI:1.7` (soft-depend) |
| Language | Java 21 |
| Build | Gradle (Groovy DSL) |
| Tests | JUnit 5 (`junit-bom:5.11.4`) |
| Group ID | `dev.linqfy` |
| Plugin main class | `dev.linqfy.bigCasares.BigCasares` |

Never change the Minecraft version, Java version, or core dependency versions without explicit approval.

---

## 2. Project Layout

```
src/
  main/
    java/dev/linqfy/bigCasares/
      BigCasares.java              ← plugin main class
      command/                     ← top-level command routing
      items/                       ← custom item registry
      module/                      ← PluginModule interface + ModuleManager
      modules/
        <module-name>/             ← one package per feature module
    resources/
      plugin.yml
      config.yml
      *.yml                        ← other per-module YAML files
  test/
    java/dev/linqfy/bigCasares/
      modules/<module-name>/       ← mirrors the main package layout
docs/
  superpowers/
    plans/                         ← agent implementation plans (YYYY-MM-DD-<name>.md)
    specs/                         ← design specs (YYYY-MM-DD-<name>-design.md)
resourcepack/                      ← server resource pack assets
```

Stick to this layout. New modules go in `modules/<module-name>/`.

---

## 3. Module System

Every feature lives inside a `PluginModule` implementation.

```java
public interface PluginModule {
    String getId();   // stable kebab-case id, e.g. "bounty-system"
    void onEnable();
    void onDisable();
}
```

### Module rules

- The module id **must be stable** — tests depend on it. Use kebab-case (e.g. `"shop-system"`, `"bounty-system"`).
- All Bukkit wiring (listener registration, command registration, scheduler tasks) happens inside `onEnable()`.
- `onDisable()` must release any resources the module holds.
- Keep Bukkit code **thin** in the module class. Push domain logic into a dedicated `*Service` class.
- A module enables conditionally based on `config.yml`. Read the relevant `modules.<module-id>.enabled` key in `ModuleManager`.
- Register the module in `BigCasares#initializeRuntime()` and expose a getter if other modules need to call it.
- Each module is self-contained. Do not reference another module's internal classes — use the getter on the main plugin class.

### Module file structure pattern (required)

```
modules/<name>/
  <Name>Module.java          ← PluginModule, wires everything
  <Name>Service.java         ← core domain logic (no Bukkit imports)
  <Name>Settings.java        ← immutable config record
  <Name>Storage.java         ← persistence interface
  Yaml<Name>Storage.java     ← YAML implementation of the storage interface
  <Name>Listener.java        ← Bukkit event listener(s), delegates to Service
  <Name>PlayerState.java     ← per-player domain state (if needed)
  <Name>*.java               ← any additional domain types
```

---

## 4. Code Style

### General

- **No comments** unless they explain non-obvious domain logic. Let class and method names be the documentation.
- Use `final` on classes that are not meant to be extended.
- Use `final` on fields whenever possible.
- Prefer immutable value types (records or final classes with only-read accessors).
- Use Java records for simple data carriers (`BountySettings`, `BountyProcessingResult`, etc.).
- No mutable statics. No global singletons beyond the plugin main class.

### Naming

- Packages: `dev.linqfy.bigCasares.modules.<moduleName>` (camelCase module name, matches existing pattern).
- Classes: `PascalCase`, prefixed with the module name (e.g. `BountyService`, `MissionModule`).
- Methods: `camelCase`, verb-first for actions (`handlePlayerKill`, `stackGeneratedBounty`).
- Config keys: `kebab-case` in YAML, mirroring the module id (`bounty-system.economy.take-percent`).
- Module ids: `kebab-case` string literal returned by `getId()`.

### Formatting

- 4-space indent, no tabs.
- UTF-8 encoding everywhere (`options.encoding = 'UTF-8'` is already set in `build.gradle`).
- One blank line between methods.
- Imports: no wildcard imports.

### Bukkit / Paper specific

- Always use `plugin.getLogger()` for logging, never `System.out`.
- User-facing messages use `§` color codes for now (no Adventure components unless the whole plugin migrates).
- Register listeners only in `onEnable()` via `plugin.getServer().getPluginManager().registerEvents(listener, plugin)`.
- Never call `Bukkit.getServer()` in domain/service code — pass what you need through constructors.

### Vault / Economy

- Wrap Vault behind a small gateway interface (e.g. `BountyEconomy`, `RewardGateway`) so services are testable without a server.
- Use `resolveOrThrow()` pattern in the module's `onEnable()` to fail fast if Vault is unavailable.

---

## 5. Test-Driven Development

**All new code must be written test-first.** Write the test, watch it fail, then implement.

### Non-negotiable TDD rules

1. Write failing tests **before** the production class.
2. Every `*Service` class must have a `*ServiceTest` covering all meaningful branches.
3. Every `*Module` must have a `*ModuleWiringTest` that at minimum verifies `getId()` returns the correct stable id.
4. Every `Yaml*Storage` must have a `Yaml*StorageTest` that writes and reads from a temp directory.
5. Every settings/config record with validation logic must have tests covering valid and invalid construction.

### Test structure

- Mirror the `src/main` package structure under `src/test`.
- Test class name: `<SubjectClass>Test`.
- Test method name: describes the **behaviour** in plain English, e.g. `paysExistingBountyAndRefreshesFromRemainingBalance`.
- Use `@Test` (JUnit 5). No JUnit 4 annotations.
- Use `assertEquals`, `assertTrue`, `assertThrows` from `org.junit.jupiter.api.Assertions`.

### Fakes and in-memory doubles

- Do **not** use Mockito or any mocking framework. Write small fake/in-memory implementations instead.
- Pattern: define a private static final class inside the test class that implements the interface under test.
- Example:

```java
private static final class InMemoryBountyStorage implements BountyStorage {
    private final Map<UUID, BountyPlayerState> states = new HashMap<>();
    @Override public Optional<BountyPlayerState> load(UUID id) { ... }
    @Override public void save(BountyPlayerState state) { ... }
}
```

### What does NOT need tests

- Pure Bukkit listener dispatch code (event routing that only delegates to a service method).
- The main `BigCasares.java` plugin class bootstrap.
- Resource pack asset metadata.

### Running tests

```
./gradlew test
```

All tests must pass before any code is considered done.

---

## 6. Superpowers Directory

Before writing a single line of code, an agent (or human) working on a new module **must** create two documents:

### `docs/superpowers/specs/YYYY-MM-DD-<module-name>-design.md`

A design spec covering:
- **Objective** — what the module does and why.
- **Scope** — what is explicitly in and out of scope.
- **Confirmed rules** — the business/gameplay rules that drive the logic.
- **Architecture** — key classes and their responsibilities (no code, just descriptions).
- **Config shape** — YAML keys this module reads/writes.
- **Boundaries** — what this module must not touch.

### `docs/superpowers/plans/YYYY-MM-DD-<module-name>.md`

A task-by-task implementation plan covering:
- **Goal** — one paragraph summary.
- **Tech Stack** — list the versions in use.
- **File Structure** — new files and modified files, with a one-line description of each.
- **Boundaries** — what must not be changed.
- **Tasks** — numbered, each with:
  - files affected
  - step-by-step instructions with checkbox (`- [ ]`) sub-steps
  - verification criteria (how to know the step is done)

The plan header must include:

```markdown
> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
```

Plans and specs are **committed alongside the code** that implements them.

---

## 7. Agent Tagging Requirements

When an AI agent completes a module or meaningful contribution, it **must** include the following at the bottom of the relevant plan file (`docs/superpowers/plans/...`):

```markdown
---

## Agent Completion Report

**Agent:** <agent name or model identifier>
**Date completed:** YYYY-MM-DD
**Branch:** <branch name>

### What was built
- Bullet list of every file created or modified.

### Tests written
- Bullet list of every test class, with a one-line summary of what it covers.

### Testing instructions
How to run the tests for this module:
```
./gradlew test --tests "dev.linqfy.bigCasares.modules.<module>.*"
```
Expected result: all tests pass, no failures.

### Deviations from plan
- Any steps that were changed, skipped, or added, and why.

### Known limitations
- Anything left out of scope or flagged for future work.
```

This tag is required. It provides an audit trail and helps human reviewers understand what was done.

---

## 8. Build & Run

### Build (required before pushing)

```bash
./gradlew clean build
```

Expected: `BUILD SUCCESSFUL`

### Run tests only

```bash
./gradlew test
```

### Run local Paper server (requires Paper downloaded by the plugin)

```bash
./gradlew runServer
```

Minecraft version is pinned to `1.21.11` in `build.gradle`.

### Encoding

All source and resource files use UTF-8.

---

## 9. Config & Resources

### `config.yml`

- Each module has a top-level key `modules.<module-id>.enabled: true/false`.
- Module-specific settings go under a key matching the module id:

```yaml
modules:
  bounty-system:
    enabled: true

bounty-system:
  economy:
    take-percent: 0.30
    bounty-percent: 0.20
    minimum-balance: 100.0
    minimum-bounty: 25.0
```

- Provide sensible defaults in code when reading config values (`getDouble("key", default)`).

### `plugin.yml`

- Declare every command under `commands:`.
- Declare every permission under `permissions:`, with a description and default.
- `api-version: '1.21'`
- `softdepend: [Vault]` — Vault is optional at server level, each module fails fast at runtime if it requires it.

### Per-module YAML files

- Store per-player data under `plugins/BigCasares/data/<module-name>/players/<uuid>.yml`.
- Use `Path`-based IO, not `File`-based where possible.
- One file per player per module.

### Resource pack

- Custom item textures live in `resourcepack/`.
- Custom model data is managed through `ModelDataUtil` and `CustomItemRegistry`.
- Every custom item must be registered in `CustomItemRegistry` in `BigCasares#initializeRuntime()`.

---

## 10. Do & Do Not

### Do

- Follow the `PluginModule` pattern for every new feature.
- Write the spec and plan before writing code.
- Write tests before production code (TDD).
- Use fake/in-memory doubles in tests, not mocks.
- Keep domain logic in `*Service` classes free of Bukkit imports.
- Wrap Vault behind a small testable gateway interface.
- Commit spec + plan + code + tests together in the same PR/branch.
- Tag your agent completion report in the plan file when done.
- Run `./gradlew clean build` before declaring work complete.

### Do Not

- Do not add logic to an existing module that belongs to a different module.
- Do not call `Bukkit.*` or `plugin.*` from inside a `*Service` class.
- Do not use Mockito or any mocking framework.
- Do not use wildcard imports.
- Do not persist kill history, leaderboards, or global statistics unless explicitly scoped.
- Do not change `build.gradle` dependency versions without approval.
- Do not skip writing the spec, plan, and agent completion report.
- Do not push code that does not pass `./gradlew test`.
- Do not add a new module without a corresponding `modules.<id>.enabled` config key.
