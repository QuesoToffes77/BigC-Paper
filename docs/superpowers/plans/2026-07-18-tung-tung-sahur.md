# Tung Tung Tung Sahur Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Tung Tung Tung Sahur as a raid-scaled, aggressively mobile second PvE boss with dynamic wall charges, three summoned-bat mechanics, exact damage-placement rewards, and Java/Bedrock assets.

**Architecture:** Preserve the shared PvE lifecycle and extract deterministic policies for scaling, corridor selection, hit pressure, standings, and rewards. A focused `PaperSahurCombatController` owns Sahur-only live entity mechanics while `PaperAbyssGuardianRuntime` remains the shared instance host and exposes lifecycle hooks. Boss definitions and presentation assets are selected by boss ID so the Abyss Guardian remains available.

**Tech Stack:** Java 25, Paper 26.2 API, JUnit 5, Bukkit YAML/PDC, BetterModel 3.2, Geyser custom entities, Gradle resource-pack builder.

---

## File Structure

New production units:

- `BossDefinitionCatalog.java`: immutable boss-ID lookup.
- `BossRaidScaling.java`: participant eligibility/count clamping and health formula.
- `SahurChargePlanner.java`: pure solid-wall corridor scoring.
- `SahurCombatPressure.java`: pure combo, spin, and per-player hit timing rules.
- `BossDamageRanking.java`: deterministic top-three calculation.
- `BossReward.java`, `BossRewardStore.java`, `YamlBossRewardStore.java`: exact reward values and durable pending delivery.
- `SahurRewardService.java`: online inventory/overflow and pending reward orchestration.
- `PaperSahurCombatController.java`: live movement, charge, carrier, stomp, spin, and summon mechanics.
- `SahurOwnedEntities.java`: instance-scoped bats, carriers, model handles, and scheduled-task cleanup.

Existing files modified:

- `AbyssGuardianDefinition.java` and loader: presentation model key and raid-scaling fields.
- `PaperAbyssGuardianRuntime.java`: configurable model key, damage timestamps, lifecycle/combat hooks.
- `PveBossModule.java`: load both YAML definitions and route spawn by ID.
- `BigCasaresCommand.java`: `/boss spawn boss-id` dispatch and suggestions.
- `ItemCatalogLoader.java`, `CustomItemDefinition.java`, `CatalogItemStackFactory.java`: custom attack attributes.
- `ItemCatalogModule.java`: seed `sahurs_bat.yml`.
- `GeyserIntegrationModule.java`: Sahur marker-to-entity mapping.
- Resource-pack registry and Java/Bedrock assets: Sahur boss, summoned bat, and reward bat.

### Task 1: Multi-boss definition and spawn routing

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossDefinitionCatalog.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossDefinitionCatalogTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/AbyssGuardianDefinition.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/AbyssGuardianDefinitionLoader.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperAbyssGuardianRuntime.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModuleWiringTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java`

- [ ] **Step 1: Write failing catalog and command tests**

```java
@Test
void resolvesBothConfiguredBossesByNormalizedId() {
    var abyss = TestBossDefinitions.definition("abyss-guardian", "bigcasares:abyss_guardian");
    var sahur = TestBossDefinitions.definition("tung-tung-sahur", "bigcasares:boss_sahur");
    var catalog = new BossDefinitionCatalog(List.of(abyss, sahur));

    assertSame(abyss, catalog.require("ABYSS-GUARDIAN"));
    assertSame(sahur, catalog.require("tung-tung-sahur"));
    assertEquals(List.of("abyss-guardian", "tung-tung-sahur"), catalog.ids());
}

@Test
void bossSuggestionsExposeSahurWithoutRemovingAbyssGuardian() {
    assertEquals(
        List.of("abyss-guardian", "tung-tung-sahur"),
        BigCasaresCommand.bossIdSuggestions("")
    );
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.BossDefinitionCatalogTest" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`

Expected: compilation fails because `BossDefinitionCatalog`, the presentation model field, and `bossIdSuggestions` do not exist.

- [ ] **Step 3: Implement ID lookup and configurable presentation key**

```java
public final class BossDefinitionCatalog {
    private final Map<String, AbyssGuardianDefinition> byId;

    public BossDefinitionCatalog(Collection<AbyssGuardianDefinition> definitions) {
        LinkedHashMap<String, AbyssGuardianDefinition> values = new LinkedHashMap<>();
        for (AbyssGuardianDefinition definition : definitions) {
            String id = normalize(definition.id());
            if (values.putIfAbsent(id, definition) != null) {
                throw new IllegalArgumentException("duplicate boss id: " + id);
            }
        }
        byId = Map.copyOf(values);
    }

    public AbyssGuardianDefinition require(String id) {
        AbyssGuardianDefinition definition = byId.get(normalize(id));
        if (definition == null) throw new IllegalArgumentException("Boss desconocido: " + id);
        return definition;
    }

    public List<String> ids() { return byId.keySet().stream().sorted().toList(); }

    private static String normalize(String id) {
        return Objects.requireNonNull(id, "id").strip().toLowerCase(Locale.ROOT);
    }
}
```

Add `String javaModelKey` to the definition record, load `presentation.java-model-key`, pass it to `javaModels.attach`, retain one runtime per definition in `PveBossModule`, and expose `spawn(String bossId, Location location)`. Keep `spawnAbyssGuardian` as a compatibility delegate.

- [ ] **Step 4: Make the two concrete boss IDs route through `PveBossModule.spawn`**

```java
public static List<String> bossIdSuggestions(String prefix) {
    String lowered = prefix.toLowerCase(Locale.ROOT);
    return List.of("abyss-guardian", "tung-tung-sahur").stream()
        .filter(id -> id.startsWith(lowered)).toList();
}
```

Change usage text to `Uso: /boss spawn abyss-guardian|tung-tung-sahur` and report the configured display name after spawning.

- [ ] **Step 5: Run tests and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.*" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`

Expected: all selected tests pass and existing Abyss Guardian wiring assertions remain green.

- [ ] **Step 6: Commit only task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss src/test/java/dev/linqfy/bigCasares/modules/pveboss src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java
git commit -m "feat: support multiple PvE boss definitions"
```

### Task 2: Raid health scaling

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossRaidScaling.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossRaidScalingTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/AbyssGuardianDefinition.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/AbyssGuardianDefinitionLoader.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperAbyssGuardianRuntime.java`

- [ ] **Step 1: Write failing formula tests**

```java
@ParameterizedTest
@CsvSource({"0,60000", "10,60000", "15,80000", "20,100000", "35,100000"})
void sahurHealthClampsRaidBetweenTenAndTwenty(int players, double expected) {
    BossRaidScaling scaling = new BossRaidScaling(20_000.0, 4_000.0, 10, 20);
    assertEquals(expected, scaling.maximumHealth(players));
}
```

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.BossRaidScalingTest"`

Expected: compilation fails because `BossRaidScaling` does not exist.

- [ ] **Step 3: Implement immutable scaling and spawn-time snapshot**

```java
public record BossRaidScaling(double baseHealth, double healthPerPlayer, int minimumPlayers, int maximumPlayers) {
    public BossRaidScaling {
        if (baseHealth <= 0 || healthPerPlayer < 0 || minimumPlayers < 1 || maximumPlayers < minimumPlayers)
            throw new IllegalArgumentException("invalid raid scaling");
    }

    public int clampedParticipants(int actual) {
        return Math.clamp(actual, minimumPlayers, maximumPlayers);
    }

    public double maximumHealth(int actual) {
        return baseHealth + healthPerPlayer * clampedParticipants(actual);
    }
}
```

Load `raid-scaling.enabled/base-health/health-per-player/minimum-players/maximum-players`. At spawn, count alive non-spectator players in `audience-radius`, compute once, and construct both `BossHealthPool` and vanilla capped Warden health from that value.

- [ ] **Step 4: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.BossRaidScalingTest" --tests "dev.linqfy.bigCasares.modules.pveboss.AbyssGuardianDefinitionLoaderTest"`

Expected: selected tests pass, including YAML defaults for the non-scaling Abyss Guardian.

- [ ] **Step 5: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss src/test/java/dev/linqfy/bigCasares/modules/pveboss
git commit -m "feat: scale Sahur health for raid size"
```

### Task 3: Dynamic wall-corridor planning

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargePlanner.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargePlannerTest.java`

- [ ] **Step 1: Write failing corridor tests**

```java
@Test
void choosesSolidWallCorridorContainingMostPlayers() {
    var planner = new SahurChargePlanner(8.0, 40.0, 2.25);
    List<SahurChargePlanner.Corridor> corridors = List.of(
        corridor(1, 0, 20, true), corridor(0, 1, 16, true), corridor(-1, 0, 30, false));
    List<BossPosition> players = List.of(pos(5, 1), pos(9, -1), pos(0, 7));

    assertEquals(corridors.getFirst(), planner.choose(pos(0, 0), corridors, players).orElseThrow());
}

@Test
void rejectsOpenAirAndWallsOutsideDistanceBounds() {
    var planner = new SahurChargePlanner(8.0, 40.0, 2.25);
    assertTrue(planner.choose(pos(0, 0), List.of(
        corridor(1, 0, 7, true), corridor(0, 1, 41, true), corridor(-1, 0, 20, false)), List.of()).isEmpty());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurChargePlannerTest"`

Expected: compilation fails because the planner does not exist.

- [ ] **Step 3: Implement pure segment scoring**

```java
public Optional<Corridor> choose(BossPosition origin, List<Corridor> candidates, List<BossPosition> players) {
    return candidates.stream()
        .filter(Corridor::solidImpact)
        .filter(c -> c.length() >= minimumLength && c.length() <= maximumLength)
        .max(Comparator.comparingInt((Corridor c) -> playersInCorridor(origin, c, players))
            .thenComparing(Comparator.comparingDouble(Corridor::length).reversed()));
}
```

Use horizontal point-to-segment distance and exclude points behind the start or beyond the wall. Paper adaptation will produce 24 compass rays plus bearings to player-cluster centroids.

- [ ] **Step 4: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurChargePlannerTest"`

Expected: all planner cases pass.

- [ ] **Step 5: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargePlanner.java src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargePlannerTest.java
git commit -m "feat: plan Sahur charges against arena walls"
```

### Task 4: Combat pressure and deterministic damage ranking

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurCombatPressure.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossDamageRanking.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossDamageContribution.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurCombatPressureTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossDamageRankingTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperAbyssGuardianRuntime.java`

- [ ] **Step 1: Write failing pressure and tie-order tests**

```java
@Test
void spinTriggersForFourNearbyPlayersOrFourRecentCriticalHits() {
    var pressure = new SahurCombatPressure(Duration.ofSeconds(2), 4, Duration.ofMillis(500));
    assertTrue(pressure.shouldSpin(4, List.of()));
    assertTrue(pressure.shouldSpin(1, List.of(t(0), t(200), t(500), t(1_900))));
    assertFalse(pressure.shouldSpin(3, List.of(t(0), t(200), t(500))));
}

@Test
void equalDamageUsesTimeTotalWasReached() {
    UUID first = UUID.randomUUID(), second = UUID.randomUUID();
    var ranking = new BossDamageRanking().topThree(List.of(
        new BossDamageContribution(second, 100, Instant.parse("2026-07-18T00:00:02Z")),
        new BossDamageContribution(first, 100, Instant.parse("2026-07-18T00:00:01Z"))));
    assertEquals(List.of(first, second), ranking.stream().map(BossDamageStanding::playerId).toList());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurCombatPressureTest" --tests "dev.linqfy.bigCasares.modules.pveboss.BossDamageRankingTest"`

Expected: compilation fails for the new policy types.

- [ ] **Step 3: Implement bounded timing rules and ranking**

Implement `mayHit(playerId, now)` with a 10-tick/500 ms cooldown, `comboLength(RandomGenerator)` returning 1, 2, or 3 using 0.35 then 0.15 continuation rolls, and standings sorted by damage descending, `reachedAt` ascending, then UUID string ascending. Record `reachedAt = now` after every positive final-damage merge.

- [ ] **Step 4: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurCombatPressureTest" --tests "dev.linqfy.bigCasares.modules.pveboss.BossDamageRankingTest"`

Expected: all timing, combo cap, zero-damage exclusion, and deterministic ranking tests pass.

- [ ] **Step 5: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss src/test/java/dev/linqfy/bigCasares/modules/pveboss
git commit -m "feat: add Sahur combat pressure policies"
```

### Task 5: Sahur's Bat catalog item and exact asset bytes

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/items/catalog/ItemCombatDefinition.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/CustomItemDefinition.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogLoader.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/catalog/CatalogItemStackFactory.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/items/ItemCatalogModule.java`
- Create: `src/main/resources/content/items/sahurs_bat.yml`
- Create: `resourcepack/java/assets/bigcasares/items/sahurs_bat.json`
- Create: `resourcepack/java/assets/bigcasares/models/item/sahurs_bat.json`
- Create: `resourcepack/java/assets/bigcasares/textures/item/sahurs_bat.png`
- Create: `resourcepack/bedrock/textures/item/sahurs_bat.png`
- Modify: `resourcepack/bedrock/textures/item_texture.json`
- Modify: `resourcepack/shared/registry.yml`
- Modify: `resourcepack/assets/bigcasares/lang/en_us.json`
- Modify: `resourcepack/assets/bigcasares/lang/es_es.json`
- Test: `src/test/java/dev/linqfy/bigCasares/items/catalog/ItemCatalogLoaderTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/items/catalog/DefaultItemCatalogTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurAssetContractTest.java`

- [ ] **Step 1: Write failing item/asset contract tests**

```java
@Test
void sahurBatHasIronSwordDamageAndDoubleAttackSpeed() {
    CustomItemDefinition bat = load("sahurs_bat.yml");
    assertEquals("IRON_SWORD", bat.material());
    assertEquals(6.0, bat.combat().orElseThrow().attackDamage());
    assertEquals(3.2, bat.combat().orElseThrow().attackSpeed());
    assertTrue(bat.recipeDefinition().isEmpty());
}

@Test
void everySahurTextureIsTheUploadedSakurBytes() throws Exception {
    byte[] expected = Files.readAllBytes(Path.of("agent_uploads/boss1/sakur.png"));
    assertArrayEquals(expected, Files.readAllBytes(Path.of("resourcepack/java/assets/bigcasares/textures/item/sahurs_bat.png")));
    assertArrayEquals(expected, Files.readAllBytes(Path.of("resourcepack/bedrock/textures/item/sahurs_bat.png")));
    assertArrayEquals(expected, Files.readAllBytes(Path.of("resourcepack/bedrock/textures/boss/sahur.png")));
}
```

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.items.catalog.*" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurAssetContractTest"`

Expected: missing combat schema, catalog entry, and asset files cause failures.

- [ ] **Step 3: Add explicit combat schema and Bukkit attributes**

```java
public record ItemCombatDefinition(double attackDamage, double attackSpeed) {
    public ItemCombatDefinition {
        if (!Double.isFinite(attackDamage) || attackDamage < 0) throw new IllegalArgumentException("attackDamage");
        if (!Double.isFinite(attackSpeed) || attackSpeed <= 0) throw new IllegalArgumentException("attackSpeed");
    }
}
```

Load `components.combat.attack-damage` and `attack-speed`. In `CatalogItemStackFactory`, install main-hand modifiers whose final displayed values are exactly 6.0 and 3.2, using stable `NamespacedKey`s so reconciliation replaces rather than duplicates modifiers.

Use this catalog entry:

```yaml
id: sahurs_bat
mechanic: sahurs-bat
material: IRON_SWORD
item-model: bigcasares:sahurs_bat
legacy-custom-model-data: 1010
display:
  translation-key: item.bigcasares.sahurs_bat
  fallback-name: "Bate de Sahur"
  lore: ["§7Premio al mayor daño contra Tung Tung Tung Sahur."]
max-stack-size: 1
components:
  max-damage: 250
  combat:
    attack-damage: 6.0
    attack-speed: 3.2
appearance:
  java-item-definition: java/assets/bigcasares/items/sahurs_bat.json
  bedrock-texture: bedrock/textures/item/sahurs_bat.png
```

- [ ] **Step 4: Copy supplied assets through `apply_patch` and register them**

Install the supplied `sahurs_bat.json` as the Java model, create the 1.21 item-definition selector for `bigcasares:item/sahurs_bat`, and copy the exact `sakur.png` bytes to every declared target. Add `sahurs-bat` to `resourcepack/shared/registry.yml` and seed `sahurs_bat.yml` from `ItemCatalogModule.DEFAULT_FILES`.

- [ ] **Step 5: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.items.catalog.*" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurAssetContractTest"`

Expected: catalog, attribute, model-reference, and byte-identity tests pass.

- [ ] **Step 6: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/items/catalog src/main/java/dev/linqfy/bigCasares/modules/items/ItemCatalogModule.java src/main/resources/content/items/sahurs_bat.yml resourcepack src/test/java/dev/linqfy/bigCasares/items/catalog src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurAssetContractTest.java
git commit -m "feat: add Sahur bat reward item"
```

### Task 6: Boss and summoned-bat model integration

**Files:**
- Create: `build/run-server/plugins/BetterModel/models/boss_sahur.bbmodel`
- Create: `build/run-server/plugins/BetterModel/models/bat_boss.bbmodel`
- Create: `resourcepack/bedrock/entity/sahur.entity.json`
- Create: `resourcepack/bedrock/entity/sahur_bat.entity.json`
- Create: `resourcepack/bedrock/models/entity/sahur.geo.json`
- Create: `resourcepack/bedrock/models/entity/sahur_bat.geo.json`
- Create: `resourcepack/bedrock/animations/sahur.animation.json`
- Create: `resourcepack/bedrock/animations/sahur_bat.animation.json`
- Modify: `resourcepack/bedrock/animation_controllers/bigcasares.controller.json`
- Create: `resourcepack/bedrock/textures/boss/sahur.png`
- Create: `resourcepack/bedrock/textures/boss/sahur_bat.png`
- Modify: `resourcepack/shared/registry.yml`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/model/JavaModelKeys.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserIntegrationModule.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurAssetContractTest.java`

- [ ] **Step 1: Extend the failing asset contract**

Assert the Java Sahur model contains `idle`, `walk`, `sprint`, `bat_hit`, `spin_in_place`, and `stomp`; the bat model contains `rise_type_throw`, `spin_type_throw`, `type_shield`, and `type_launch_attack`; both Bedrock entity identifiers are registered; all boss/bat textures equal uploaded `sakur.png` bytes.

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurAssetContractTest"`

Expected: missing installed models, `sprint`, entity files, and registry entries fail.

- [ ] **Step 3: Install and repair the provided model sources**

Copy `boss_sahur.bbmodel` and `bat_boss.bbmodel` from `agent_uploads/boss1`. Add a BetterModel `sprint` animation using the provided Bedrock sprint keyframes mapped to the matching model bones; do not change geometry, UVs, or existing animations. Register constants `SAHUR = "boss_sahur"` and `SAHUR_BAT = "bat_boss"`.

- [ ] **Step 4: Install Bedrock exports and controllers**

Use the supplied geometry/animation JSON as source, split into the repo's entity files, and bind animation queries for idle/walk/sprint/bat-hit/spin/stomp and all four bat states. Register marker values `tung-tung-sahur` and `tung-tung-sahur-bat` in the Geyser integration.

- [ ] **Step 5: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurAssetContractTest" --tests "dev.linqfy.bigCasares.modules.geyser.*"`

Expected: animation, texture-byte, registry, and mapping tests pass.

- [ ] **Step 6: Commit task files**

```powershell
git add build/run-server/plugins/BetterModel/models resourcepack src/main/java/dev/linqfy/bigCasares/modules/model/JavaModelKeys.java src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserIntegrationModule.java src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurAssetContractTest.java
git commit -m "feat: integrate Sahur entity models"
```

### Task 7: Sahur YAML and grounded combat controller

**Files:**
- Create: `src/main/resources/bosses/tung-tung-sahur.yml`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperSahurCombatController.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurOwnedEntities.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperAbyssGuardianRuntime.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurDefinitionTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurOwnedEntitiesTest.java`

- [ ] **Step 1: Write failing definition and cleanup tests**

Assert the YAML loads with ID/model/scaling, all configured animation IDs exist, required abilities have exact damage/cooldowns, and closing `SahurOwnedEntities` closes every registered handle once even when one closer throws.

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurDefinitionTest" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurOwnedEntitiesTest"`

Expected: missing YAML/controller/owner types fail.

- [ ] **Step 3: Add exact ability configuration**

Configure walk as the default loop; bat hit at 14 damage with controller-driven combo; stomp at 6 damage/eight-block radius; spin for 80 ticks at 5 damage per 10-tick victim interval; charge at 18-second base cooldown and 24-tick telegraph; hunter bat, shield bats, and launch bat with phase gates described by the design. Set phase thresholds to 0.66 and 0.33 and reduce coordinator cooldowns only through existing phase-three enrage behavior.

- [ ] **Step 4: Implement controller lifecycle and grounded attacks**

`PaperSahurCombatController` receives the instance UUID, Warden, model handle, definition, eligible-player supplier, animation callback, damage callback, and `SahurOwnedEntities`. Its 1-tick task chooses frantic navigation destinations every 20-60 ticks, executes forward bat-hit boxes, expanding brown `Particle.DUST` stomp rings, and the 80-tick moving spin using `SahurCombatPressure.mayHit`.

- [ ] **Step 5: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurDefinitionTest" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurOwnedEntitiesTest" --tests "dev.linqfy.bigCasares.modules.pveboss.PveBossModuleWiringTest"`

Expected: YAML contract, owner cleanup, and dual-runtime wiring pass.

- [ ] **Step 6: Commit task files**

```powershell
git add src/main/resources/bosses/tung-tung-sahur.yml src/main/java/dev/linqfy/bigCasares/modules/pveboss src/test/java/dev/linqfy/bigCasares/modules/pveboss
git commit -m "feat: add Sahur grounded combat"
```

### Task 8: Wall charge and victim carriers

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperSahurCombatController.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargeVictims.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargeVictimsTest.java`

- [ ] **Step 1: Write failing carrier lifecycle tests**

Use fake carrier handles to prove one carrier per player, duplicate pickup rejection, impact release with 16 damage, cleanup release with zero damage, and logout/death/world-change release of only the affected victim.

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurChargeVictimsTest"`

Expected: missing victim registry fails compilation.

- [ ] **Step 3: Implement carrier registry and live charge**

`SahurChargeVictims` stores `Map<UUID, CarrierHandle>` and exposes `pickup`, `release(playerId, reason)`, and `releaseAll(reason)`. In Paper, spawn an invisible, invulnerable, silent, AI-disabled, saddled Pig per victim; add the player as passenger; teleport carriers along the boss charge offsets. Ray trace every tick so the first solid collision ends the charge. Apply 16 raw damage and outward knockback only for `WALL_IMPACT`; all other reasons dismount without impact damage.

- [ ] **Step 4: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurChargePlannerTest" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurChargeVictimsTest"`

Expected: corridor and victim lifecycle suites pass.

- [ ] **Step 5: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperSahurCombatController.java src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargeVictims.java src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurChargeVictimsTest.java
git commit -m "feat: add Sahur wall charge"
```

### Task 9: Hunter, shield, and launch bats

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurHunterBatRoute.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurShieldFormation.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurLaunchTracker.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperSahurCombatController.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurHunterBatRouteTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurShieldFormationTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurLaunchTrackerTest.java`

- [ ] **Step 1: Write failing summon-policy tests**

Assert hunter routing selects the farthest unvisited target and stops at four; shield offsets always contain five points in a boss-facing arc and intercept only player-fired `AbstractArrow` segment crossings; launch tracking caps attributable fall damage at 10 and clears state on landing/logout/timeout.

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurHunterBatRouteTest" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurShieldFormationTest" --tests "dev.linqfy.bigCasares.modules.pveboss.SahurLaunchTrackerTest"`

Expected: missing summon-policy classes fail compilation.

- [ ] **Step 3: Implement pure summon policies**

Hunter route stores visited UUIDs and selects `max(distanceSquared)` among eligible unvisited players. Shield formation returns five offsets at angles `-50, -25, 0, 25, 50` degrees and radius 2.5. Launch tracker records player/start time, returns `min(originalFallDamage, 10.0)`, and expires after 15 seconds.

- [ ] **Step 4: Implement live bat displays/models**

Spawn invisible marker anchors with `bat_boss` model handles. Hunter: play `rise_type_throw` for 15 ticks, loop `spin_type_throw`, home between up to four targets, and use a per-target hit cooldown. Shield: spawn exactly five `type_shield` anchors for 160 ticks, orient toward the five-second arrow-pressure vector, and remove intersecting player-fired arrows. Launch: telegraph for 20 ticks, spawn below the chosen player, play `type_launch_attack`, apply vertical velocity for an 18-block apex, and cap its next attributable fall event.

- [ ] **Step 5: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.Sahur*Test"`

Expected: all Sahur policy and lifecycle tests pass.

- [ ] **Step 6: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss src/test/java/dev/linqfy/bigCasares/modules/pveboss
git commit -m "feat: add Sahur bat summons"
```

### Task 10: Exact placement rewards and durable pending delivery

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossReward.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossRewardStore.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/YamlBossRewardStore.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/SahurRewardService.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperAbyssGuardianRuntime.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/SahurRewardServiceTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/YamlBossRewardStoreTest.java`

- [ ] **Step 1: Write failing exact-reward and exactly-once tests**

```java
@Test
void prizesMatchDamagePlacementsExactly() {
    assertEquals(new BossReward("sahurs_bat", 1), BossReward.forSahurPlacement(1));
    assertEquals(new BossReward("ENCHANTED_GOLDEN_APPLE", 3), BossReward.forSahurPlacement(2));
    assertEquals(new BossReward("ECHO_SHARD", 3), BossReward.forSahurPlacement(3));
}

@Test
void disconnectedWinnerReceivesPendingRewardOnlyOnce() {
    service.award(winner, new BossReward("sahurs_bat", 1));
    assertEquals(1, store.pending(winner).size());
    service.deliverPending(winner);
    service.deliverPending(winner);
    assertEquals(1, inventory.deliveredCount("sahurs_bat"));
    assertTrue(store.pending(winner).isEmpty());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurRewardServiceTest" --tests "dev.linqfy.bigCasares.modules.pveboss.YamlBossRewardStoreTest"`

Expected: missing reward types/store/service fail compilation.

- [ ] **Step 3: Implement write-before-deliver persistence**

Store reward records by unique reward UUID under `boss-rewards.yml`. `award` persists first, then attempts online delivery. `deliverPending` claims one record under synchronization, gives the item, writes overflow drops through the delivery gateway, removes the record only after success, and saves. On failure, retain the record for the next join.

- [ ] **Step 4: Wire death leaderboard and join delivery**

On Sahur death, use `BossDamageRanking.topThree`, map placements to the exact rewards, publish Spanish placement/damage/prize messages, and call `SahurRewardService.award`. Resolve `sahurs_bat` from the active custom-item catalog/factory; use vanilla `ItemStack`s for enchanted golden apples and echo shards. Register a `PlayerJoinEvent` listener for pending delivery.

- [ ] **Step 5: Run and verify GREEN**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.SahurRewardServiceTest" --tests "dev.linqfy.bigCasares.modules.pveboss.YamlBossRewardStoreTest" --tests "dev.linqfy.bigCasares.modules.pveboss.BossDamageRankingTest"`

Expected: exact values, offline persistence, overflow, failure retry, and duplicate prevention tests pass.

- [ ] **Step 6: Commit task files**

```powershell
git add src/main/java/dev/linqfy/bigCasares/modules/pveboss src/test/java/dev/linqfy/bigCasares/modules/pveboss
git commit -m "feat: reward Sahur damage leaders"
```

### Task 11: Resource-pack build and complete regression verification

**Files:**
- Modify only files implicated by failing verification.

- [ ] **Step 1: Run all focused boss, catalog, resource-pack, command, and Geyser tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.pveboss.*" --tests "dev.linqfy.bigCasares.items.catalog.*" --tests "dev.linqfy.bigCasares.modules.resourcepack.*" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest" --tests "dev.linqfy.bigCasares.modules.geyser.*"`

Expected: zero failed tests. Fix production code for any behavioral failure; do not weaken assertions.

- [ ] **Step 2: Generate both resource packs with BetterModel output**

Use BetterModel's generated Java pack at `build/run-server/plugins/BetterModel/build.zip` after model reload/startup, then run:

```powershell
gradle generateResourcePacks -PbettermodelJavaPack=build/run-server/plugins/BetterModel/build.zip
```

Expected: exit 0; generated Java and Bedrock archives exist under `build/generated-resourcepacks`; archive inspection contains Sahur, summoned bat, reward item, animations, and exact `sakur.png` bytes.

- [ ] **Step 3: Run the full test suite**

Run: `gradle test`

Expected: exit 0 and zero failed tests. Do not run `gradle clean`.

- [ ] **Step 4: Start Paper and verify live registration**

Run the existing Paper runtime without deleting `build/run-server`, wait for the `Done` startup marker, and inspect logs for:

```text
PvE Boss System listo
Tung Tung Tung Sahur cargado
BetterModel
```

Execute `/boss spawn tung-tung-sahur` through the available server console harness and verify no missing-model, missing-animation, YAML, Geyser, or resource-pack exceptions appear. Stop the server gracefully.

- [ ] **Step 5: Review requirements and working-tree scope**

Compare every design-spec section to the tests and generated archives. Run `git diff --check`, inspect `git status --short`, and confirm unrelated pre-existing changes were not reverted or staged.

- [ ] **Step 6: Commit final verification fixes**

If verification required a Sahur-specific correction, inspect it with `git diff -- path/to/corrected/file`, stage only that exact file or Sahur hunk with `git add -p`, inspect `git diff --cached`, and commit it with `git commit -m "test: verify Tung Tung Tung Sahur integration"`. If verification changed nothing, do not create an empty commit.

Never stage unrelated user changes or Gradle cache files.
