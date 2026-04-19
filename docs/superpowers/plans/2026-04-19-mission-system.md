# Mission System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a `mission-system` module for BigCasares with per-player daily and weekly missions, Vault-based rewards, Spanish container HUDs, command-only access, and a configurable initial catalog.

**Architecture:** Add a new plugin module that boots only when Vault economy is available, then split the feature into focused units: config/catalog loading, player state storage, domain services for assignment and claiming, Bukkit listeners for progress, and inventory-based HUD navigation. Keep Bukkit-specific code thin and push mission rotation, claiming, and catalog validation into unit-testable services.

**Tech Stack:** Java 21, Spigot API 1.21.11, VaultAPI 1.7, JUnit 5, Bukkit YAML configuration/storage

---

## File Structure

### Create

- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionModule.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionScope.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionDefinition.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionProgressSnapshot.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionAssignment.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionPlayerState.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCatalog.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCatalogLoader.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionType.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionClock.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionRotationPolicy.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/PlayerMissionService.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionStorage.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/YamlMissionStorage.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/VaultEconomyGateway.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionProgressEngine.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionHudView.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionHudController.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCommand.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionListener.java`
- `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionTexts.java`

### Modify

- `build.gradle`
- `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- `src/main/resources/config.yml`
- `src/main/resources/plugin.yml`
- `src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleCommand.java`

### Test

- `src/test/java/dev/linqfy/bigCasares/modules/missions/MissionRotationPolicyTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/missions/MissionCatalogLoaderTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/missions/PlayerMissionServiceTest.java`
- `src/test/java/dev/linqfy/bigCasares/modules/missions/YamlMissionStorageTest.java`

### Notes

- Keep command routing in `CopperAppleCommand` for now because the plugin already exposes a single root command.
- Avoid introducing database dependencies; state stays in YAML files under the plugin data folder.

## Task 1: Add Vault Dependency And Module Wiring

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/plugin.yml`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionModule.java`

- [ ] **Step 1: Write the failing wiring test**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("mission-system", new MissionModule(null).getId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionModuleWiringTest"`
Expected: FAIL because `MissionModule` does not exist yet

- [ ] **Step 3: Write minimal implementation**

```java
package dev.linqfy.bigCasares.modules.missions;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;

public final class MissionModule implements PluginModule {

    private final BigCasares plugin;

    public MissionModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "mission-system";
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }
}
```

- [ ] **Step 4: Wire dependency and registration**

```gradle
repositories {
    mavenCentral()
    maven { url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/" }
    maven { url = "https://jitpack.io" }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}
```

```yaml
softdepend: [Vault]

permissions:
  bigcasares.missions.open:
    description: Open the missions HUD
    default: true
  bigcasares.missions.claim:
    description: Claim mission rewards
    default: true
  bigcasares.missions.admin:
    description: Manage player mission rotations
    default: op
```

```yaml
modules:
  copper-apple: true
  mission-system: true
```

```java
moduleManager.register(new CopperAppleModule(this));
moduleManager.register(new MissionModule(this));
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionModuleWiringTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/main/resources/plugin.yml src/main/resources/config.yml src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/java/dev/linqfy/bigCasares/modules/missions/MissionModule.java src/test/java/dev/linqfy/bigCasares/modules/missions/MissionModuleWiringTest.java
git commit -m "feat: wire mission system module"
```

## Task 2: Build Mission Domain And Rotation Policy

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionScope.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionDefinition.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionProgressSnapshot.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionAssignment.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionPlayerState.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionType.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionClock.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionRotationPolicy.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/missions/MissionRotationPolicyTest.java`

- [ ] **Step 1: Write the failing rotation tests**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MissionRotationPolicyTest {

    @Test
    void assignsConfiguredAmountPerScope() {
        MissionCatalog catalog = MissionCatalog.of(
            List.of(
                definition("daily-a", MissionScope.DAILY),
                definition("daily-b", MissionScope.DAILY),
                definition("daily-c", MissionScope.DAILY),
                definition("weekly-a", MissionScope.WEEKLY),
                definition("weekly-b", MissionScope.WEEKLY)
            )
        );

        MissionRotationPolicy policy = new MissionRotationPolicy(new Random(7));

        MissionPlayerState state = policy.createFreshState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            catalog,
            2,
            1,
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        assertEquals(2, state.dailyAssignments().size());
        assertEquals(1, state.weeklyAssignments().size());
    }

    @Test
    void usesPlayerSeedSoDifferentPlayersCanGetDifferentSelections() {
        MissionCatalog catalog = MissionCatalog.of(
            List.of(
                definition("daily-a", MissionScope.DAILY),
                definition("daily-b", MissionScope.DAILY),
                definition("daily-c", MissionScope.DAILY)
            )
        );

        MissionRotationPolicy policy = new MissionRotationPolicy(new Random(99));

        MissionPlayerState first = policy.createFreshState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            catalog,
            2,
            0,
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );
        MissionPlayerState second = policy.createFreshState(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            catalog,
            2,
            0,
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        assertNotEquals(first.dailyAssignments().keySet(), second.dailyAssignments().keySet());
    }

    private static MissionDefinition definition(String id, MissionScope scope) {
        return new MissionDefinition(
            id,
            scope,
            "Titulo " + id,
            "Descripcion " + id,
            MissionType.HOLD_EXACT_ITEM_COUNT,
            67,
            100.0,
            1,
            true,
            Map.of("material", "TUFF")
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionRotationPolicyTest"`
Expected: FAIL because the mission domain classes do not exist

- [ ] **Step 3: Write minimal implementation**

```java
public enum MissionScope {
    DAILY,
    WEEKLY
}
```

```java
public enum MissionType {
    HOLD_EXACT_ITEM_COUNT,
    WAX_BLOCK_COUNT,
    FINAL_HIT_PLAYER_WITH_ITEM,
    CROUCH_ON_SLEEPING_BED,
    RENAME_ITEM_TO_EXACT_NAME,
    NAME_ENTITY_AFTER_PLAYER,
    EQUIP_SPECIFIC_ITEM,
    STAND_ON_BLOCK_AT_Y,
    KILL_ENTITY_WITH_ITEM_ONLY
}
```

```java
public record MissionDefinition(
    String id,
    MissionScope scope,
    String title,
    String description,
    MissionType type,
    int goal,
    double reward,
    int weight,
    boolean enabled,
    Map<String, Object> params
) {}
```

```java
public record MissionProgressSnapshot(int progress, boolean completed, boolean claimed) {}
```

```java
public record MissionAssignment(
    MissionDefinition definition,
    MissionProgressSnapshot snapshot
) {}
```

```java
public record MissionPlayerState(
    UUID playerId,
    Instant generatedAt,
    Instant dailyResetsAt,
    Instant weeklyResetsAt,
    Map<String, MissionAssignment> dailyAssignments,
    Map<String, MissionAssignment> weeklyAssignments
) {}
```

```java
public final class MissionCatalog {
    private final List<MissionDefinition> definitions;

    private MissionCatalog(List<MissionDefinition> definitions) {
        this.definitions = List.copyOf(definitions);
    }

    public static MissionCatalog of(List<MissionDefinition> definitions) {
        return new MissionCatalog(definitions);
    }

    public List<MissionDefinition> byScope(MissionScope scope) {
        return definitions.stream()
            .filter(MissionDefinition::enabled)
            .filter(definition -> definition.scope() == scope)
            .toList();
    }
}
```

```java
public final class MissionRotationPolicy {
    private final Random random;

    public MissionRotationPolicy(Random random) {
        this.random = random;
    }

    public MissionPlayerState createFreshState(UUID playerId, MissionCatalog catalog, int dailyCount, int weeklyCount, Instant now, Instant dailyReset, Instant weeklyReset) {
        return new MissionPlayerState(
            playerId,
            now,
            dailyReset,
            weeklyReset,
            select(playerId, catalog.byScope(MissionScope.DAILY), dailyCount),
            select(playerId, catalog.byScope(MissionScope.WEEKLY), weeklyCount)
        );
    }
}
```

- [ ] **Step 4: Finish deterministic selection logic**

```java
private Map<String, MissionAssignment> select(UUID playerId, List<MissionDefinition> pool, int count) {
    List<MissionDefinition> shuffled = new ArrayList<>(pool);
    Collections.shuffle(shuffled, new Random(random.nextLong() ^ playerId.getMostSignificantBits() ^ playerId.getLeastSignificantBits()));

    return shuffled.stream()
        .limit(Math.min(count, shuffled.size()))
        .collect(Collectors.toMap(
            MissionDefinition::id,
            definition -> new MissionAssignment(definition, new MissionProgressSnapshot(0, false, false)),
            (left, right) -> left,
            LinkedHashMap::new
        ));
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionRotationPolicyTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions src/test/java/dev/linqfy/bigCasares/modules/missions/MissionRotationPolicyTest.java
git commit -m "feat: add mission rotation domain"
```

## Task 3: Load Mission Catalog From Config

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCatalogLoader.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/missions/MissionCatalogLoaderTest.java`
- Modify: `src/main/resources/config.yml`

- [ ] **Step 1: Write the failing loader tests**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionCatalogLoaderTest {

    @Test
    void loadsEnabledMissionDefinitionsFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mission-system.missions.daily-bed.scope", "daily");
        config.set("mission-system.missions.daily-bed.title", "Salto de cama");
        config.set("mission-system.missions.daily-bed.description", "Agachate 5 veces arriba de una cama ocupada.");
        config.set("mission-system.missions.daily-bed.type", "CROUCH_ON_SLEEPING_BED");
        config.set("mission-system.missions.daily-bed.goal", 5);
        config.set("mission-system.missions.daily-bed.reward", 250.0);
        config.set("mission-system.missions.daily-bed.weight", 1);
        config.set("mission-system.missions.daily-bed.enabled", true);
        config.set("mission-system.missions.daily-bed.params", java.util.Map.of());

        MissionCatalog catalog = new MissionCatalogLoader().load(config);

        assertEquals(1, catalog.byScope(MissionScope.DAILY).size());
        assertEquals("Salto de cama", catalog.byScope(MissionScope.DAILY).getFirst().title());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionCatalogLoaderTest"`
Expected: FAIL because `MissionCatalogLoader` does not exist

- [ ] **Step 3: Write minimal implementation**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MissionCatalogLoader {

    public MissionCatalog load(FileConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("mission-system.missions");
        if (root == null) {
            return MissionCatalog.of(List.of());
        }

        List<MissionDefinition> definitions = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            definitions.add(new MissionDefinition(
                id,
                MissionScope.valueOf(section.getString("scope", "daily").toUpperCase()),
                section.getString("title", id),
                section.getString("description", ""),
                MissionType.valueOf(section.getString("type")),
                section.getInt("goal"),
                section.getDouble("reward"),
                section.getInt("weight", 1),
                section.getBoolean("enabled", true),
                section.getConfigurationSection("params") == null ? Map.of() : section.getConfigurationSection("params").getValues(false)
            ));
        }

        return MissionCatalog.of(definitions);
    }
}
```

- [ ] **Step 4: Seed the config with initial mission entries**

```yaml
mission-system:
  daily:
    count: 3
  weekly:
    count: 2
  missions:
    daily-bed-jumper:
      scope: daily
      title: "Salto de cama"
      description: "Subite arriba de la cama de alguien durmiendo y agachate 5 veces."
      type: CROUCH_ON_SLEEPING_BED
      goal: 5
      reward: 250.0
      weight: 1
      enabled: true
      params: {}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionCatalogLoaderTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCatalogLoader.java src/test/java/dev/linqfy/bigCasares/modules/missions/MissionCatalogLoaderTest.java src/main/resources/config.yml
git commit -m "feat: load mission catalog from config"
```

## Task 4: Add Player Mission Service For Rotation, Progress, And Claiming

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/PlayerMissionService.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/missions/PlayerMissionServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMissionServiceTest {

    @Test
    void marksMissionCompletedWhenProgressReachesGoal() {
        MissionCatalog catalog = MissionCatalog.of(List.of(
            new MissionDefinition("tuff-67", MissionScope.DAILY, "Toba exacta", "Consegui 67 bloques de toba", MissionType.HOLD_EXACT_ITEM_COUNT, 67, 100.0, 1, true, Map.of("material", "TUFF"))
        ));

        PlayerMissionService service = new PlayerMissionService(
            catalog,
            new MissionRotationPolicy(new Random(5)),
            () -> Instant.parse("2026-04-19T10:00:00Z"),
            1,
            0,
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        MissionPlayerState state = service.getOrCreateState(playerId, null);
        MissionPlayerState updated = service.updateAbsoluteProgress(state, MissionScope.DAILY, "tuff-67", 67);

        assertTrue(updated.dailyAssignments().get("tuff-67").snapshot().completed());
        assertEquals(67, updated.dailyAssignments().get("tuff-67").snapshot().progress());
    }

    @Test
    void marksClaimedAfterSuccessfulClaim() {
        MissionCatalog catalog = MissionCatalog.of(List.of(
            new MissionDefinition("tuff-67", MissionScope.DAILY, "Toba exacta", "Consegui 67 bloques de toba", MissionType.HOLD_EXACT_ITEM_COUNT, 67, 100.0, 1, true, Map.of("material", "TUFF"))
        ));

        FakeEconomyGateway economy = new FakeEconomyGateway();
        PlayerMissionService service = new PlayerMissionService(
            catalog,
            new MissionRotationPolicy(new Random(5)),
            () -> Instant.parse("2026-04-19T10:00:00Z"),
            1,
            0,
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z")
        );

        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        MissionPlayerState state = service.updateAbsoluteProgress(service.getOrCreateState(playerId, null), MissionScope.DAILY, "tuff-67", 67);

        MissionPlayerState claimed = service.claimAllCompleted(playerId, state, economy);

        assertTrue(claimed.dailyAssignments().get("tuff-67").snapshot().claimed());
        assertEquals(100.0, economy.totalDeposited);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.PlayerMissionServiceTest"`
Expected: FAIL because `PlayerMissionService` and `FakeEconomyGateway` do not exist

- [ ] **Step 3: Write minimal implementation**

```java
package dev.linqfy.bigCasares.modules.missions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class PlayerMissionService {
    private final MissionCatalog catalog;
    private final MissionRotationPolicy rotationPolicy;
    private final Supplier<Instant> clock;
    private final int dailyCount;
    private final int weeklyCount;
    private final Instant dailyReset;
    private final Instant weeklyReset;

    public PlayerMissionService(MissionCatalog catalog, MissionRotationPolicy rotationPolicy, Supplier<Instant> clock, int dailyCount, int weeklyCount, Instant dailyReset, Instant weeklyReset) {
        this.catalog = catalog;
        this.rotationPolicy = rotationPolicy;
        this.clock = clock;
        this.dailyCount = dailyCount;
        this.weeklyCount = weeklyCount;
        this.dailyReset = dailyReset;
        this.weeklyReset = weeklyReset;
    }

    public MissionPlayerState getOrCreateState(UUID playerId, MissionPlayerState state) {
        if (state != null) {
            return state;
        }
        return rotationPolicy.createFreshState(playerId, catalog, dailyCount, weeklyCount, clock.get(), dailyReset, weeklyReset);
    }
}
```

- [ ] **Step 4: Add update and claim behavior**

```java
public MissionPlayerState updateAbsoluteProgress(MissionPlayerState state, MissionScope scope, String missionId, int progress) {
    return withUpdatedAssignment(state, scope, missionId, assignment -> {
        boolean completed = progress >= assignment.definition().goal();
        return new MissionAssignment(
            assignment.definition(),
            new MissionProgressSnapshot(progress, completed, assignment.snapshot().claimed())
        );
    });
}

public MissionPlayerState claimAllCompleted(UUID playerId, MissionPlayerState state, RewardGateway rewardGateway) {
    MissionPlayerState current = state;
    for (MissionAssignment assignment : state.dailyAssignments().values()) {
        if (assignment.snapshot().completed() && !assignment.snapshot().claimed()) {
            rewardGateway.deposit(playerId, assignment.definition().reward());
            current = withUpdatedAssignment(current, MissionScope.DAILY, assignment.definition().id(), value ->
                new MissionAssignment(value.definition(), new MissionProgressSnapshot(value.snapshot().progress(), true, true))
            );
        }
    }
    return current;
}
```

```java
interface RewardGateway {
    void deposit(UUID playerId, double amount);
}
```

```java
final class FakeEconomyGateway implements RewardGateway {
    double totalDeposited;

    @Override
    public void deposit(UUID playerId, double amount) {
        totalDeposited += amount;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.PlayerMissionServiceTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions/PlayerMissionService.java src/test/java/dev/linqfy/bigCasares/modules/missions/PlayerMissionServiceTest.java
git commit -m "feat: add player mission service"
```

## Task 5: Persist Player States In YAML

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionStorage.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/YamlMissionStorage.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/missions/YamlMissionStorageTest.java`

- [ ] **Step 1: Write the failing storage tests**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlMissionStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsPlayerState() {
        MissionDefinition definition = new MissionDefinition("tuff-67", MissionScope.DAILY, "Toba exacta", "Consegui 67 bloques de toba", MissionType.HOLD_EXACT_ITEM_COUNT, 67, 100.0, 1, true, Map.of("material", "TUFF"));
        MissionPlayerState state = new MissionPlayerState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            Instant.parse("2026-04-19T10:00:00Z"),
            Instant.parse("2026-04-20T00:00:00Z"),
            Instant.parse("2026-04-26T00:00:00Z"),
            new LinkedHashMap<>(Map.of("tuff-67", new MissionAssignment(definition, new MissionProgressSnapshot(67, true, true)))),
            Map.of()
        );

        YamlMissionStorage storage = new YamlMissionStorage(tempDir);
        storage.save(state);

        MissionPlayerState loaded = storage.load(state.playerId()).orElseThrow();

        assertEquals(67, loaded.dailyAssignments().get("tuff-67").snapshot().progress());
        assertTrue(loaded.dailyAssignments().get("tuff-67").snapshot().claimed());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.YamlMissionStorageTest"`
Expected: FAIL because storage classes do not exist

- [ ] **Step 3: Write minimal implementation**

```java
package dev.linqfy.bigCasares.modules.missions;

import java.util.Optional;
import java.util.UUID;

public interface MissionStorage {
    Optional<MissionPlayerState> load(UUID playerId);

    void save(MissionPlayerState state);
}
```

```java
package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class YamlMissionStorage implements MissionStorage {
    private final Path root;

    public YamlMissionStorage(Path root) {
        this.root = root;
    }

    @Override
    public Optional<MissionPlayerState> load(UUID playerId) {
        Path file = root.resolve(playerId + ".yml");
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        // Map fields back into MissionPlayerState
        return Optional.of(/* parsed state */);
    }

    @Override
    public void save(MissionPlayerState state) {
        try {
            Files.createDirectories(root);
            YamlConfiguration yaml = new YamlConfiguration();
            // Write state fields
            yaml.save(root.resolve(state.playerId() + ".yml").toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to save mission state", ex);
        }
    }
}
```

- [ ] **Step 4: Replace placeholders with full YAML mapping**

```java
yaml.set("player-id", state.playerId().toString());
yaml.set("generated-at", state.generatedAt().toString());
yaml.set("daily-resets-at", state.dailyResetsAt().toString());
yaml.set("weekly-resets-at", state.weeklyResetsAt().toString());
writeAssignments(yaml.createSection("daily"), state.dailyAssignments());
writeAssignments(yaml.createSection("weekly"), state.weeklyAssignments());
```

```java
private void writeAssignments(ConfigurationSection root, Map<String, MissionAssignment> assignments) {
    for (Map.Entry<String, MissionAssignment> entry : assignments.entrySet()) {
        ConfigurationSection section = root.createSection(entry.getKey());
        section.set("definition-id", entry.getValue().definition().id());
        section.set("scope", entry.getValue().definition().scope().name());
        section.set("title", entry.getValue().definition().title());
        section.set("description", entry.getValue().definition().description());
        section.set("type", entry.getValue().definition().type().name());
        section.set("goal", entry.getValue().definition().goal());
        section.set("reward", entry.getValue().definition().reward());
        section.set("weight", entry.getValue().definition().weight());
        section.set("enabled", entry.getValue().definition().enabled());
        section.set("params", entry.getValue().definition().params());
        section.set("progress", entry.getValue().snapshot().progress());
        section.set("completed", entry.getValue().snapshot().completed());
        section.set("claimed", entry.getValue().snapshot().claimed());
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.YamlMissionStorageTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions/MissionStorage.java src/main/java/dev/linqfy/bigCasares/modules/missions/YamlMissionStorage.java src/test/java/dev/linqfy/bigCasares/modules/missions/YamlMissionStorageTest.java
git commit -m "feat: persist player mission state"
```

## Task 6: Add Vault Reward Gateway And Mission Bootstrapping

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/VaultEconomyGateway.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionModule.java`

- [ ] **Step 1: Write the failing gateway test**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VaultEconomyGatewayTest {

    @Test
    void formatsAmountWithFallbackWhenEconomyUnavailable() {
        assertEquals("$100.00", VaultEconomyGateway.formatFallback(100.0, "$"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.VaultEconomyGatewayTest"`
Expected: FAIL because `VaultEconomyGateway` does not exist

- [ ] **Step 3: Write minimal implementation**

```java
package dev.linqfy.bigCasares.modules.missions;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

public final class VaultEconomyGateway implements RewardGateway {
    private final JavaPlugin plugin;
    private final Economy economy;

    public VaultEconomyGateway(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public static String formatFallback(double amount, String symbol) {
        return symbol + String.format(java.util.Locale.US, "%.2f", amount);
    }

    public static Economy resolveOrThrow(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            throw new IllegalStateException("Vault no esta instalado.");
        }

        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null || provider.getProvider() == null) {
            throw new IllegalStateException("Vault esta presente pero no hay proveedor de economia.");
        }

        return provider.getProvider();
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(playerId);
        EconomyResponse response = economy.depositPlayer(offlinePlayer, amount);
        if (!response.transactionSuccess()) {
            throw new IllegalStateException("No se pudo depositar la recompensa: " + response.errorMessage);
        }
    }
}
```

- [ ] **Step 4: Bootstrap the full mission service in the module**

```java
public void onEnable() {
    Economy economy = VaultEconomyGateway.resolveOrThrow(plugin);
    MissionCatalog catalog = new MissionCatalogLoader().load(plugin.getConfig());
    YamlMissionStorage storage = new YamlMissionStorage(plugin.getDataFolder().toPath().resolve("data").resolve("missions").resolve("players"));
    PlayerMissionService service = new PlayerMissionService(
        catalog,
        new MissionRotationPolicy(new Random()),
        Instant::now,
        plugin.getConfig().getInt("mission-system.daily.count", 3),
        plugin.getConfig().getInt("mission-system.weekly.count", 2),
        Instant.now().plusSeconds(86400),
        Instant.now().plusSeconds(604800)
    );
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.VaultEconomyGatewayTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions/VaultEconomyGateway.java src/main/java/dev/linqfy/bigCasares/modules/missions/MissionModule.java src/test/java/dev/linqfy/bigCasares/modules/missions/VaultEconomyGatewayTest.java
git commit -m "feat: add vault mission rewards"
```

## Task 7: Add Command Routing And Container HUDs

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionHudView.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionHudController.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCommand.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleCommand.java`

- [ ] **Step 1: Write the failing command parsing test**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionCommandTest {

    @Test
    void parsesDailyShortcut() {
        assertEquals(MissionHudView.DAILY_LIST, MissionCommand.resolveView(new String[]{"misiones", "diarias"}));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionCommandTest"`
Expected: FAIL because mission command classes do not exist

- [ ] **Step 3: Write minimal implementation**

```java
public enum MissionHudView {
    MAIN_MENU,
    DAILY_LIST,
    WEEKLY_LIST,
    DETAIL,
    CLAIMABLES
}
```

```java
public final class MissionCommand {
    public static MissionHudView resolveView(String[] args) {
        if (args.length < 2) {
            return MissionHudView.MAIN_MENU;
        }
        return switch (args[1].toLowerCase(java.util.Locale.ROOT)) {
            case "diarias", "diario" -> MissionHudView.DAILY_LIST;
            case "semanales", "semanal" -> MissionHudView.WEEKLY_LIST;
            case "reclamar" -> MissionHudView.CLAIMABLES;
            default -> MissionHudView.MAIN_MENU;
        };
    }
}
```

- [ ] **Step 4: Implement Bukkit inventory HUDs and route `/bc misiones`**

```java
if ("misiones".equalsIgnoreCase(args[0])) {
    if (!(sender instanceof Player player)) {
        sender.sendMessage(ChatColor.RED + "Solo jugadores pueden abrir el HUD de misiones.");
        return true;
    }
    missionHudController.open(player, MissionCommand.resolveView(args));
    return true;
}
```

```java
public void open(Player player, MissionHudView view) {
    Inventory inventory = Bukkit.createInventory(player, 54, switch (view) {
        case MAIN_MENU -> "Misiones";
        case DAILY_LIST -> "Misiones diarias";
        case WEEKLY_LIST -> "Misiones semanales";
        case CLAIMABLES -> "Recompensas listas";
        case DETAIL -> "Detalle de mision";
    });
    player.openInventory(inventory);
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionCommandTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions/MissionHudView.java src/main/java/dev/linqfy/bigCasares/modules/missions/MissionHudController.java src/main/java/dev/linqfy/bigCasares/modules/missions/MissionCommand.java src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleCommand.java src/test/java/dev/linqfy/bigCasares/modules/missions/MissionCommandTest.java
git commit -m "feat: add mission HUD command routing"
```

## Task 8: Add Mission Progress Tracking And Spanish Catalog

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionProgressEngine.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionListener.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/missions/MissionTexts.java`
- Modify: `src/main/resources/config.yml`

- [ ] **Step 1: Write the failing progress test**

```java
package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionProgressEngineTest {

    @Test
    void returnsAbsoluteProgressForExactCountMission() {
        MissionDefinition definition = new MissionDefinition("tuff-67", MissionScope.DAILY, "Toba exacta", "Consegui exactamente 67 de toba", MissionType.HOLD_EXACT_ITEM_COUNT, 67, 100.0, 1, true, Map.of("material", "TUFF"));

        MissionProgressEngine engine = new MissionProgressEngine();

        assertEquals(67, engine.resolveAbsoluteProgress(definition, Map.of("material", "TUFF", "count", 67)));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionProgressEngineTest"`
Expected: FAIL because `MissionProgressEngine` does not exist

- [ ] **Step 3: Write minimal implementation**

```java
package dev.linqfy.bigCasares.modules.missions;

import java.util.Map;

public final class MissionProgressEngine {
    public int resolveAbsoluteProgress(MissionDefinition definition, Map<String, Object> context) {
        return switch (definition.type()) {
            case HOLD_EXACT_ITEM_COUNT -> Integer.parseInt(String.valueOf(context.getOrDefault("count", 0)));
            default -> 0;
        };
    }
}
```

- [ ] **Step 4: Add listener hooks and the full Spanish mission catalog**

```yaml
mission-system:
  daily:
    count: 3
  weekly:
    count: 2
  missions:
    tuff-67:
      scope: daily
      title: "Toba exacta"
      description: "Consegui exactamente 67 bloques de toba."
      type: HOLD_EXACT_ITEM_COUNT
      goal: 67
      reward: 175.0
      weight: 1
      enabled: true
      params:
        material: TUFF
```

Add the remaining mission entries to reach roughly 45 daily and 9 weekly definitions in Spanish, including:

- final hit on a player with `WOODEN_SWORD` or `STICK`
- rename `NETHERITE_HOE` to `Tel Aviv 2027`
- name a pig after a player
- wear a cursed pumpkin
- stand on diorite at Y 155
- kill an iron golem with kelp only
- wax 67 copper blocks

- [ ] **Step 5: Run targeted tests to verify they pass**

Run: `./gradlew test --tests "dev.linqfy.bigCasares.modules.missions.MissionProgressEngineTest" --tests "dev.linqfy.bigCasares.modules.missions.MissionCatalogLoaderTest" --tests "dev.linqfy.bigCasares.modules.missions.PlayerMissionServiceTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/missions/MissionProgressEngine.java src/main/java/dev/linqfy/bigCasares/modules/missions/MissionListener.java src/main/java/dev/linqfy/bigCasares/modules/missions/MissionTexts.java src/main/resources/config.yml src/test/java/dev/linqfy/bigCasares/modules/missions/MissionProgressEngineTest.java
git commit -m "feat: add mission progress tracking and catalog"
```

## Task 9: Full Verification

**Files:**
- Verify only

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: PASS with all mission tests and existing copper apple tests green

- [ ] **Step 2: Build the plugin jar**

Run: `./gradlew build`
Expected: PASS and generate plugin artifact

- [ ] **Step 3: Smoke-check the plugin metadata**

Run: `Get-Content src/main/resources/plugin.yml`
Expected: `softdepend: [Vault]`, mission permissions, and command metadata present

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat: add mission system module"
```

## Self-Review

### Spec coverage

- Vault-required startup is covered by Tasks 1 and 6.
- Per-player daily and weekly rotation is covered by Tasks 2 and 4.
- YAML persistence is covered by Task 5.
- Container HUDs and command-only access are covered by Task 7.
- Spanish configurable catalog with roughly six weeks of content is covered by Task 8.
- Test/build verification is covered by Task 9.

### Placeholder scan

- The only remaining broad instruction is the catalog expansion in Task 8. During execution, replace it with actual YAML mission entries rather than a partial sample.
- The storage parsing note in Task 5 must be replaced by real code before marking the task complete.

### Type consistency

- `RewardGateway` is the service interface used by `PlayerMissionService`; `VaultEconomyGateway` implements it.
- `MissionHudView` is the routing enum consumed by `MissionCommand` and `MissionHudController`.
- `MissionRotationPolicy#createFreshState(...)` is the single entry point for fresh assignments.
