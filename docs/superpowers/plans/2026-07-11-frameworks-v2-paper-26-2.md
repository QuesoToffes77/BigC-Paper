# BigCasares Frameworks V2 on Paper 26.2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Upgrade BigCasares to Minecraft Java/Paper 26.2 on Java 25 and deliver the Resource Pack, Team, Nexus, Entity Shop, PvE Boss, and optional Geyser modules required by Frameworks V2.

**Architecture:** Keep Bukkit/Paper and optional-plugin calls behind gateways while domain services remain plain Java. Modules are registered in dependency order (`resource-pack-system`, `team-system`, `nexus-system`, `entity-shop-system`, `pve-boss-system`, `geyser-integration`), use the existing `PluginModule`/`ModuleManager` lifecycle, and share a `ClientPlatformGateway` plus the resource registry. Geyser and PlaceholderAPI integrations are soft dependencies with safe Java-only fallbacks.

**Tech Stack:** Java 25, Paper API 26.2 build 56-alpha, Gradle 9-compatible Groovy DSL, JUnit 5, Bukkit YAML, Adventure, optional PlaceholderAPI/Geyser APIs, Java and Bedrock resource-pack formats.

---

### Task 1: Pin the Minecraft 26.2 platform contract

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/PlatformVersionConfigurationTest.java`
- Modify: `build.gradle`
- Modify: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `src/main/resources/plugin.yml`

- [x] **Step 1: Write a failing configuration test** that reads the three files and asserts `paper-api:26.2.build.56-alpha`, `minecraftVersion("26.2")`, `targetJavaVersion = 25`, Gradle 9.2.1, and `api-version: '26.2'`.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.PlatformVersionConfigurationTest"`; it must fail against the current 1.21.11/Java 21 configuration.
- [x] **Step 3: Replace Spigot API with the pinned Paper API**, add the official Paper Maven repository, update run-paper to 3.0.2, set Java 25, update the wrapper, and set the plugin API version.
- [x] **Step 4: Verify GREEN** with the same targeted test, then run `gradle compileJava` to expose real 26.2 API changes.

### Task 2: Build deterministic Java and Bedrock resource packs

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackAsset.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackManifest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackSettings.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackValidator.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/JavaResourcePackBuilder.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/BedrockResourcePackBuilder.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackBuilder.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackPublisher.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackPlayerState.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackStatusListener.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackBuilderTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackValidatorTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackModuleWiringTest.java`
- Create: `resourcepack/shared/registry.yml`
- Create: `resourcepack/bedrock/manifest.json`
- Modify: `build.gradle`

- [x] **Step 1: Write failing tests** for both output files, missing assets, duplicate logical IDs, stable Bedrock UUIDs, deterministic Java SHA-1, and unchanged-input build skipping.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.modules.resourcepack.*"`.
- [x] **Step 3: Implement immutable registry parsing and validation.** Java and Bedrock builders receive independent roots, sort paths before zipping, normalize ZIP timestamps, hash actual bytes, and preserve UUIDs from the checked-in Bedrock manifest.
- [x] **Step 4: Add `generateResourcePacks` to `build.gradle`.** It must produce `build/generated-resourcepacks/bigcasares-java.zip`, `bigcasares-bedrock.mcpack`, `manifest.json`, and `checksums.yml`, and `build` must depend on it.
- [x] **Step 5: Implement Java delivery and status tracking.** Only Java clients receive the configured external URL; visual-dependent features query `ResourcePackService.hasLoadedPack(UUID)`; rejection only kicks when `required` is true.
- [x] **Step 6: Verify GREEN** with the targeted suite and `gradle generateResourcePacks` twice, checking identical checksums and unchanged UUIDs.

### Task 3: Add the optional platform and Geyser boundary

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/platform/ClientPlatform.java`
- Create: `src/main/java/dev/linqfy/bigCasares/platform/ClientPlatformGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserSettings.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/BedrockEntityGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserPlatformGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserResourcePackGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserCustomItemGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserCustomEntityGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserShopFormGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserNexusVisualGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserBossVisualGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserIntegrationModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/geyser/GeyserIntegrationModuleTest.java`

- [x] **Step 1: Write a failing no-Geyser test** proving the module can be constructed, reports Java/unknown, and leaves all Bedrock features disabled without loading a Geyser class.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.modules.geyser.*"`.
- [x] **Step 3: Implement capability discovery once at enable time.** Keep Geyser types out of public module APIs; expose explicit capability flags for forms, pack registration, custom items, and custom entities; each experimental capability logs one warning and falls back independently.
- [x] **Step 4: Verify GREEN** and later prove the whole plugin starts with no Geyser JAR installed.

### Task 4: Add teams, tags, presentation, commands, and placeholders

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamId.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamRole.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamColor.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/Team.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamPresentation.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamNamePresentationGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamPresentationService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/YamlTeamStorage.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/BukkitTeamNamePresentationGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/BigCasaresTeamPlaceholderExpansion.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/teams/TeamModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamTagValidationTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamPresentationServiceTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/teams/TeamPlaceholderExpansionTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java`

- [x] **Step 1: Write failing tests** for uppercase 2-5 character tags, global uniqueness, color changes, the exact `bold -> reset -> nickname color` prefix boundary, no-team/offline placeholders, and owner-only mutation.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.modules.teams.*"`.
- [x] **Step 3: Implement YAML persistence and the service API**, keeping the `Team` record immutable and refreshing only affected players/teams.
- [x] **Step 4: Implement scoreboard, TAB, display-name, and optional PlaceholderAPI adapters** with config switches and one missing-plugin warning.
- [x] **Step 5: Route `/team tag`, `/team color`, and `/team appearance`** without changing account names.
- [x] **Step 6: Verify GREEN** with the team suite.

### Task 5: Replace fixed Nexus damage and add safe placement/visual recovery

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusId.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusDamageKind.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusDamageRequest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusVisualGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusDamageResolver.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusPlacementPolicy.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/JavaNexusVisualGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusListener.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusDamageResolverTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusDamageMultiplierTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusContainerClearanceTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusPlacementObstructionTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusVisualRecoveryTest.java`

- [x] **Step 1: Write failing tests** for final melee/critical/projectile/explosion damage, shooter attribution, own-team rejection, environmental defaults, multipliers, chest/double-chest/piston boundaries, open-face rules, and missing-display recovery.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.modules.nexus.*"`.
- [x] **Step 3: Implement pure damage and placement policies.** Applied damage is exactly `event.getFinalDamage() * configuredMultiplier`; attacker ownership comes only from `DamageSource`, projectile/TNT ownership, or direct entity.
- [x] **Step 4: Implement the Paper adapter.** Spawn a persistent static PDC-marked Ghast anchor plus invulnerable non-interactive ItemDisplay/TextDisplay children sharing `nexus-id`; cancel vanilla anchor damage after capturing final damage.
- [x] **Step 5: Protect the configured exclusion volume** for block place, double-chest extension, and piston movement while never creating a generic movement wall.
- [x] **Step 6: Verify GREEN** with the nexus suite.

### Task 6: Upgrade shops to platform-aware NPCs and one purchase pipeline

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopNpcType.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopNpcDefinition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopNpcFactory.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopUiGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopView.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopPurchaseRequest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopPlatformUiResolver.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/JavaInventoryShopUi.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/BedrockFormShopUi.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ChatFallbackShopUi.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopNpcDefinitionTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopNpcFactoryTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopPlatformUiResolverTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/BedrockShopFormMapperTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java`
- Modify: `src/main/resources/shop.yml`

- [x] **Step 1: Write failing tests** for Player Model/Villager definitions, mandatory Bedrock Villager fallback, UI selection, form-to-request mapping, and NPC damage cancellation.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.modules.shop.*"`.
- [x] **Step 3: Spawn Paper `Mannequin` for Java player models** using profile, equipment, valid pose, immovability, and look-at settings; spawn configured non-trading Villagers otherwise.
- [x] **Step 4: Send Java inventory, Bedrock form, or chat fallback views** but submit every click/button through the existing `ShopService` purchase path.
- [x] **Step 5: Verify GREEN** with the full shop suite.

### Task 7: Add the PvE boss presentation and ability runtime

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityDefinition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityRuntime.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityState.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossTargetSelectorDefinition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityCondition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityEffectDefinition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossTelegraphDefinition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityCoordinator.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossPresentationGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossAudioGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperBossPresentationGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperBossAudioGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityCoordinatorTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilitySelectionTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityCastTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossAbilityInterruptTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossPresentationServiceTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossMusicServiceTest.java`
- Create: `src/main/resources/bosses/abyss-guardian.yml`

- [x] **Step 1: Write failing tests** for priority selection, valid targets, one cast at a time, cooldown, interruption, phase transition, one-shot music transition, radius exit, pack fallback, and UI cleanup.
- [x] **Step 2: Verify RED** with `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.*"`.
- [x] **Step 3: Implement one coordinator per boss instance** with no task per ability. It advances READY/CASTING/EXECUTING/COOLDOWN states from one module scheduler and cancels all state on defeat/despawn.
- [x] **Step 4: Implement the shared presentation** using BossBar as the cross-platform base, throttled ActionBar, Java TextDisplay, phase dialogue, telegraphs, particles, and pack-aware custom/vanilla audio.
- [x] **Step 5: Load and run the Abyss Guardian definition** with the specified Void Pulse example and resource-registry IDs.
- [x] **Step 6: Verify GREEN** with the boss suite.

### Task 8: Wire dependency order, configuration, assets, and commands

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/module/ModuleManager.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/plugin.yml`
- Modify: `src/main/resources/shop.yml`
- Create/modify: `resourcepack/java/**`
- Create/modify: `resourcepack/bedrock/**`

- [x] **Step 1: Add a failing module-order test** proving all six V2 modules are registered once and resource/team dependencies enable before their consumers.
- [x] **Step 2: Verify RED**, then register modules in the required order and expose service getters rather than direct cross-module Bukkit calls.
- [x] **Step 3: Add the complete V2 config blocks** with safe defaults, `modules.<id>` toggles, `softdepend: [Vault, packetevents, PlaceholderAPI, Geyser-Spigot, floodgate]`, team/nexus commands, and permissions.
- [x] **Step 4: Add Java and Bedrock definitions** for nexus, abyss guardian, merchant, sounds, language entries, animation controllers, and fallbacks referenced by `registry.yml`.
- [x] **Step 5: Run every targeted module suite**, then `gradle test`.

### Task 9: Prove build and runtime compatibility

**Files:**
- Verify: `build/generated-resourcepacks/**`
- Verify: `build/libs/**`
- Verify: `run/logs/latest.log`

- [x] **Step 1: Run `gradle clean test generateResourcePacks build`** and require a zero exit code with no test failures.
- [x] **Step 2: Inspect generated packs** for required entries, stable UUIDs, deterministic SHA-1, and matching manifest/checksum metadata.
- [x] **Step 3: Start Paper 26.2 without Geyser/PlaceholderAPI** and inspect `run/logs/latest.log` until Paper reports `Done` and BigCasares reports all Java-capable modules enabled with optional fallbacks.
- [x] **Step 4: Stop the server cleanly**, confirm no stack traces during shutdown, and record the exact Paper build/Java version in `README.md`.
- [x] **Step 5: Re-read the Frameworks V2 specification line by line** and report any requirement that still needs a real Bedrock device/client rather than claiming it was locally verified.
