# Bounty System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a `bounty-system` module for BigCasares that creates stacked PvP bounties from player deaths, pays existing bounty to the killer, refreshes a new bounty from the victim's remaining balance, and burns part of the withdrawn money as economic bleed.

**Architecture:** Add a dedicated module under `modules/bounties` that follows the repo's existing module pattern. Keep Bukkit code thin by pushing all bounty math and transaction ordering into a testable `BountyService`, use a small Vault adapter for balance/deposit/withdraw, and persist only per-player active bounty totals in YAML.

**Tech Stack:** Java 21, Paper/Spigot API 1.21.11, VaultAPI 1.7, JUnit 5, Bukkit YAML configuration/storage

---

## File Structure

### New files

- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyPlayerState.java`
  Holds the persisted active bounty amount and last update timestamp for one player.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyStorage.java`
  Small persistence interface for loading and saving active bounty state.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorage.java`
  YAML-backed implementation storing one file per player.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomyGateway.java`
  Vault wrapper for reading balances, withdrawing from a victim, depositing to a killer, and formatting amounts.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomy.java`
  Small abstraction that keeps `BountyService` testable without Vault or Bukkit.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountySettings.java`
  Immutable config object for `takePercent`, `bountyPercent`, `minimumBalance`, and `minimumBounty`.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyProcessingResult.java`
  Structured result of one PvP death handling pass, used by the listener for messages.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyService.java`
  Core domain logic for payout, refresh, stack persistence, and economic bleed.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyListener.java`
  `PlayerDeathEvent` listener delegating to `BountyService`.
- `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyModule.java`
  Module bootstrap that validates Vault, loads settings, storage, service, and registers the listener.
- `src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyServiceTest.java`
  Domain tests for payout, refresh, stacking, and no-op scenarios.
- `src/test/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorageTest.java`
  Persistence tests.
- `src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyModuleWiringTest.java`
  Stable module id test and simple config validation coverage.

### Modified files

- `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
  Register and hold the new `BountyModule`.
- `src/main/resources/config.yml`
  Add the `modules.bounty-system` flag plus `bounty-system` settings/messages.

### Boundaries

- Do not add bounty logic to `MissionModule` or mission classes.
- Do not make the listener perform Vault calls directly.
- Do not persist kill history or leaderboard data.

## Task 1: Add The Domain State And Service Tests

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyServiceTest.java`

- [ ] **Step 1: Write the failing test for payout plus refresh on one kill**

```java
package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyServiceTest {

    @Test
    void paysExistingBountyAndRefreshesFromRemainingBalance() {
        InMemoryBountyStorage storage = new InMemoryBountyStorage();
        FakeEconomyGateway economy = new FakeEconomyGateway();
        BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 25.0);
        BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

        UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        storage.save(new BountyPlayerState(victimId, 150.0, Instant.parse("2026-04-19T11:00:00Z")));
        economy.setBalance(victimId, 500.0);

        BountyProcessingResult result = service.handlePlayerKill(victimId, killerId);

        assertEquals(150.0, result.paidOutAmount());
        assertEquals(100.0, result.newBountyAmount());
        assertEquals(150.0, economy.totalDepositedTo(killerId));
        assertEquals(150.0, economy.totalWithdrawnFrom(victimId));
        assertEquals(100.0, storage.load(victimId).orElseThrow().activeBounty());
        assertTrue(result.createdNewBounty());
    }

    private static final class InMemoryBountyStorage implements BountyStorage {
        private final Map<UUID, BountyPlayerState> states = new HashMap<>();

        @Override
        public java.util.Optional<BountyPlayerState> load(UUID playerId) {
            return java.util.Optional.ofNullable(states.get(playerId));
        }

        @Override
        public void save(BountyPlayerState state) {
            states.put(state.playerId(), state);
        }
    }

    private static final class FakeEconomyGateway extends BountyEconomyGateway {
        private final Map<UUID, Double> balances = new HashMap<>();
        private final Map<UUID, Double> deposits = new HashMap<>();
        private final Map<UUID, Double> withdrawals = new HashMap<>();

        FakeEconomyGateway() {
            super(null, null);
        }

        void setBalance(UUID playerId, double amount) {
            balances.put(playerId, amount);
        }

        double totalDepositedTo(UUID playerId) {
            return deposits.getOrDefault(playerId, 0.0);
        }

        double totalWithdrawnFrom(UUID playerId) {
            return withdrawals.getOrDefault(playerId, 0.0);
        }

        @Override
        public double balance(UUID playerId) {
            return balances.getOrDefault(playerId, 0.0);
        }

        @Override
        public void withdraw(UUID playerId, double amount) {
            balances.put(playerId, balance(playerId) - amount);
            withdrawals.merge(playerId, amount, Double::sum);
        }

        @Override
        public void deposit(UUID playerId, double amount) {
            deposits.merge(playerId, amount, Double::sum);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.BountyServiceTest.paysExistingBountyAndRefreshesFromRemainingBalance"`

Expected: FAIL with missing symbols for `BountyService`, `BountySettings`, `BountyPlayerState`, `BountyProcessingResult`, `BountyStorage`, or because `BountyEconomyGateway` does not yet expose the referenced API.

- [ ] **Step 3: Expand the test file with the remaining domain cases**

```java
@Test
void stacksNewBountyOnExistingStoredAmountWhenNoPayoutHappensBeforeRefresh() {
    InMemoryBountyStorage storage = new InMemoryBountyStorage();
    FakeEconomyGateway economy = new FakeEconomyGateway();
    BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 25.0);
    BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

    UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000003");

    storage.save(new BountyPlayerState(victimId, 40.0, Instant.parse("2026-04-19T11:00:00Z")));

    BountyPlayerState stacked = service.stackGeneratedBounty(victimId, 60.0);

    assertEquals(100.0, stacked.activeBounty());
}

@Test
void skipsRefreshWhenVictimBalanceIsBelowMinimumBalance() {
    InMemoryBountyStorage storage = new InMemoryBountyStorage();
    FakeEconomyGateway economy = new FakeEconomyGateway();
    BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 25.0);
    BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

    UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000004");
    UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000005");

    economy.setBalance(victimId, 80.0);

    BountyProcessingResult result = service.handlePlayerKill(victimId, killerId);

    assertEquals(0.0, result.newBountyAmount());
    assertEquals(0.0, economy.totalWithdrawnFrom(victimId));
    assertEquals(0.0, storage.load(victimId).orElseThrow().activeBounty());
}

@Test
void skipsRefreshWhenComputedBountyFallsBelowMinimumBounty() {
    InMemoryBountyStorage storage = new InMemoryBountyStorage();
    FakeEconomyGateway economy = new FakeEconomyGateway();
    BountySettings settings = new BountySettings(0.30, 0.20, 100.0, 40.0);
    BountyService service = new BountyService(storage, economy, settings, () -> Instant.parse("2026-04-19T12:00:00Z"));

    UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000006");
    UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000007");

    economy.setBalance(victimId, 150.0);

    BountyProcessingResult result = service.handlePlayerKill(victimId, killerId);

    assertEquals(0.0, result.newBountyAmount());
    assertEquals(0.0, economy.totalWithdrawnFrom(victimId));
    assertEquals(0.0, storage.load(victimId).orElseThrow().activeBounty());
}

@Test
void rejectsSettingsWhenBountyPercentExceedsTakePercent() {
    IllegalArgumentException thrown = org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> new BountySettings(0.20, 0.30, 100.0, 25.0)
    );

    assertTrue(thrown.getMessage().contains("bounty-percent"));
}
```

- [ ] **Step 4: Run test class to verify it still fails for the expected reason**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.BountyServiceTest"`

Expected: FAIL because production classes for bounty domain and service behavior do not yet exist.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyServiceTest.java
git commit -m "test: add bounty service coverage"
```

## Task 2: Implement The Domain Model And Core Service

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyPlayerState.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyStorage.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomy.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountySettings.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyProcessingResult.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyService.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyServiceTest.java`

- [ ] **Step 1: Write the minimal domain records and interface**

```java
package dev.linqfy.bigCasares.modules.bounties;

import java.time.Instant;
import java.util.UUID;

public record BountyPlayerState(
    UUID playerId,
    double activeBounty,
    Instant updatedAt
) {
    public static BountyPlayerState empty(UUID playerId, Instant updatedAt) {
        return new BountyPlayerState(playerId, 0.0, updatedAt);
    }
}
```

```java
package dev.linqfy.bigCasares.modules.bounties;

import java.util.Optional;
import java.util.UUID;

public interface BountyStorage {

    Optional<BountyPlayerState> load(UUID playerId);

    void save(BountyPlayerState state);
}
```

```java
package dev.linqfy.bigCasares.modules.bounties;

public record BountySettings(
    double takePercent,
    double bountyPercent,
    double minimumBalance,
    double minimumBounty
) {
    public BountySettings {
        if (takePercent <= 0.0) {
            throw new IllegalArgumentException("take-percent debe ser mayor que 0.");
        }
        if (bountyPercent < 0.0) {
            throw new IllegalArgumentException("bounty-percent no puede ser negativo.");
        }
        if (bountyPercent > takePercent) {
            throw new IllegalArgumentException("bounty-percent no puede ser mayor que take-percent.");
        }
    }

    public double bleedPercent() {
        return takePercent - bountyPercent;
    }
}
```

```java
package dev.linqfy.bigCasares.modules.bounties;

public record BountyProcessingResult(
    double paidOutAmount,
    double newBountyAmount,
    double totalTakenAmount,
    double bleedAmount,
    boolean createdNewBounty
) {
    public static BountyProcessingResult empty() {
        return new BountyProcessingResult(0.0, 0.0, 0.0, 0.0, false);
    }
}
```

- [ ] **Step 2: Replace the fake gateway inheritance in the test with an interface-backed fake**

```java
private static final class FakeEconomyGateway implements BountyEconomy {
    private final Map<UUID, Double> balances = new HashMap<>();
    private final Map<UUID, Double> deposits = new HashMap<>();
    private final Map<UUID, Double> withdrawals = new HashMap<>();

    void setBalance(UUID playerId, double amount) {
        balances.put(playerId, amount);
    }

    double totalDepositedTo(UUID playerId) {
        return deposits.getOrDefault(playerId, 0.0);
    }

    double totalWithdrawnFrom(UUID playerId) {
        return withdrawals.getOrDefault(playerId, 0.0);
    }

    @Override
    public double balance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0);
    }

    @Override
    public void withdraw(UUID playerId, double amount) {
        balances.put(playerId, balance(playerId) - amount);
        withdrawals.merge(playerId, amount, Double::sum);
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        deposits.merge(playerId, amount, Double::sum);
    }

    @Override
    public String format(double amount) {
        return "$" + amount;
    }
}
```

```java
package dev.linqfy.bigCasares.modules.bounties;

import java.util.UUID;

public interface BountyEconomy {

    double balance(UUID playerId);

    void withdraw(UUID playerId, double amount);

    void deposit(UUID playerId, double amount);

    String format(double amount);
}
```

- [ ] **Step 3: Implement the service minimally to satisfy the tests**

```java
package dev.linqfy.bigCasares.modules.bounties;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public final class BountyService {

    private final BountyStorage storage;
    private final BountyEconomy economy;
    private final BountySettings settings;
    private final Supplier<Instant> clock;

    public BountyService(BountyStorage storage, BountyEconomy economy, BountySettings settings, Supplier<Instant> clock) {
        this.storage = storage;
        this.economy = economy;
        this.settings = settings;
        this.clock = clock;
    }

    public BountyProcessingResult handlePlayerKill(UUID victimId, UUID killerId) {
        BountyPlayerState current = storage.load(victimId).orElse(BountyPlayerState.empty(victimId, clock.get()));
        double paidOut = current.activeBounty();

        if (paidOut > 0.0) {
            economy.deposit(killerId, paidOut);
        }

        storage.save(new BountyPlayerState(victimId, 0.0, clock.get()));

        double balance = economy.balance(victimId);
        if (balance < settings.minimumBalance()) {
            return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false);
        }

        double newBounty = balance * settings.bountyPercent();
        if (newBounty < settings.minimumBounty()) {
            return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false);
        }

        double totalTaken = balance * settings.takePercent();
        double bleed = totalTaken - newBounty;

        economy.withdraw(victimId, totalTaken);
        storage.save(new BountyPlayerState(victimId, newBounty, clock.get()));
        return new BountyProcessingResult(paidOut, newBounty, totalTaken, bleed, true);
    }

    public BountyPlayerState stackGeneratedBounty(UUID victimId, double amount) {
        BountyPlayerState current = storage.load(victimId).orElse(BountyPlayerState.empty(victimId, clock.get()));
        BountyPlayerState updated = new BountyPlayerState(victimId, current.activeBounty() + amount, clock.get());
        storage.save(updated);
        return updated;
    }
}
```

- [ ] **Step 4: Run tests to verify the service is green**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.BountyServiceTest"`

Expected: PASS

- [ ] **Step 5: Refactor the service to remove the temporary zero-save before the refresh path**

```java
public BountyProcessingResult handlePlayerKill(UUID victimId, UUID killerId) {
    BountyPlayerState current = storage.load(victimId).orElse(BountyPlayerState.empty(victimId, clock.get()));
    double paidOut = current.activeBounty();

    if (paidOut > 0.0) {
        economy.deposit(killerId, paidOut);
    }

    double balance = economy.balance(victimId);
    if (balance < settings.minimumBalance()) {
        storage.save(new BountyPlayerState(victimId, 0.0, clock.get()));
        return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false);
    }

    double newBounty = balance * settings.bountyPercent();
    if (newBounty < settings.minimumBounty()) {
        storage.save(new BountyPlayerState(victimId, 0.0, clock.get()));
        return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false);
    }

    double totalTaken = balance * settings.takePercent();
    double bleed = totalTaken - newBounty;

    economy.withdraw(victimId, totalTaken);
    BountyPlayerState updated = new BountyPlayerState(victimId, newBounty, clock.get());
    storage.save(updated);
    return new BountyProcessingResult(paidOut, newBounty, totalTaken, bleed, true);
}
```

- [ ] **Step 6: Run tests again to verify the refactor stayed green**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.BountyServiceTest"`

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyPlayerState.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyStorage.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountySettings.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyProcessingResult.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyService.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomy.java src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyServiceTest.java
git commit -m "feat: add bounty service domain"
```

## Task 3: Add YAML Persistence Tests And Implementation

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorageTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorage.java`

- [ ] **Step 1: Write the failing persistence tests**

```java
package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlBountyStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsActiveBounty() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        BountyPlayerState state = new BountyPlayerState(playerId, 845.5, Instant.parse("2026-04-19T08:34:12Z"));

        YamlBountyStorage storage = new YamlBountyStorage(tempDir);
        storage.save(state);

        BountyPlayerState loaded = storage.load(playerId).orElseThrow();

        assertEquals(845.5, loaded.activeBounty());
        assertEquals(Instant.parse("2026-04-19T08:34:12Z"), loaded.updatedAt());
    }

    @Test
    void returnsEmptyWhenPlayerFileDoesNotExist() {
        YamlBountyStorage storage = new YamlBountyStorage(tempDir);

        java.util.Optional<BountyPlayerState> loaded = storage.load(UUID.fromString("00000000-0000-0000-0000-000000000011"));

        assertEquals(java.util.Optional.empty(), loaded);
    }
}
```

- [ ] **Step 2: Run the persistence tests to verify they fail**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.YamlBountyStorageTest"`

Expected: FAIL because `YamlBountyStorage` does not exist yet.

- [ ] **Step 3: Implement the YAML storage minimally**

```java
package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class YamlBountyStorage implements BountyStorage {

    private final Path directory;

    public YamlBountyStorage(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<BountyPlayerState> load(UUID playerId) {
        Path file = fileFor(playerId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        double activeBounty = config.getDouble("active-bounty", 0.0);
        Instant updatedAt = Instant.parse(config.getString("updated-at", Instant.EPOCH.toString()));
        return Optional.of(new BountyPlayerState(playerId, activeBounty, updatedAt));
    }

    @Override
    public void save(BountyPlayerState state) {
        try {
            Files.createDirectories(directory);
            YamlConfiguration config = new YamlConfiguration();
            config.set("active-bounty", state.activeBounty());
            config.set("updated-at", state.updatedAt().toString());
            config.save(fileFor(state.playerId()).toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo guardar la bounty de " + state.playerId(), ex);
        }
    }

    private Path fileFor(UUID playerId) {
        return directory.resolve(playerId + ".yml");
    }
}
```

- [ ] **Step 4: Run the persistence tests to verify they pass**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.YamlBountyStorageTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorage.java src/test/java/dev/linqfy/bigCasares/modules/bounties/YamlBountyStorageTest.java
git commit -m "feat: add bounty yaml storage"
```

## Task 4: Add The Vault Gateway And Module Wiring Tests

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyModuleWiringTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomyGateway.java`

- [ ] **Step 1: Write the failing wiring tests**

```java
package dev.linqfy.bigCasares.modules.bounties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BountyModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("bounty-system", new BountyModule(null).getId());
    }

    @Test
    void rejectsInvalidMinimumBounty() {
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> new BountySettings(0.30, 0.20, 100.0, -1.0)
        );

        assertEquals("minimum-bounty no puede ser negativo.", thrown.getMessage());
    }
}
```

- [ ] **Step 2: Run the wiring tests to verify they fail**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.BountyModuleWiringTest"`

Expected: FAIL because `BountyModule` does not exist yet and `BountySettings` validation is incomplete.

- [ ] **Step 3: Expand settings validation and add the Vault gateway**

```java
public BountySettings {
    if (takePercent <= 0.0) {
        throw new IllegalArgumentException("take-percent debe ser mayor que 0.");
    }
    if (bountyPercent < 0.0) {
        throw new IllegalArgumentException("bounty-percent no puede ser negativo.");
    }
    if (bountyPercent > takePercent) {
        throw new IllegalArgumentException("bounty-percent no puede ser mayor que take-percent.");
    }
    if (minimumBalance < 0.0) {
        throw new IllegalArgumentException("minimum-balance no puede ser negativo.");
    }
    if (minimumBounty < 0.0) {
        throw new IllegalArgumentException("minimum-bounty no puede ser negativo.");
    }
}
```

```java
package dev.linqfy.bigCasares.modules.bounties;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;

public final class BountyEconomyGateway implements BountyEconomy {

    private final JavaPlugin plugin;
    private final Economy economy;

    public BountyEconomyGateway(JavaPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
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
    public double balance(UUID playerId) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
        return economy.getBalance(player);
    }

    @Override
    public void withdraw(UUID playerId, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            throw new IllegalStateException("No se pudo retirar dinero de la victima: " + response.errorMessage);
        }
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        OfflinePlayer player = plugin.getServer().getOfflinePlayer(playerId);
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            throw new IllegalStateException("No se pudo pagar la bounty: " + response.errorMessage);
        }
    }

    @Override
    public String format(double amount) {
        try {
            return economy.format(amount);
        } catch (RuntimeException ex) {
            return "$" + String.format(Locale.US, "%.2f", amount);
        }
    }
}
```

- [ ] **Step 4: Add the minimal module bootstrap**

```java
package dev.linqfy.bigCasares.modules.bounties;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import net.milkbowl.vault.economy.Economy;

public final class BountyModule implements PluginModule {

    private final BigCasares plugin;
    private BountyEconomyGateway economyGateway;
    private BountyService service;

    public BountyModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "bounty-system";
    }

    @Override
    public void onEnable() {
        Economy economy = BountyEconomyGateway.resolveOrThrow(plugin);
        BountySettings settings = new BountySettings(
            plugin.getConfig().getDouble("bounty-system.economy.take-percent", 0.30),
            plugin.getConfig().getDouble("bounty-system.economy.bounty-percent", 0.20),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-balance", 100.0),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-bounty", 25.0)
        );

        BountyStorage storage = new YamlBountyStorage(plugin.getDataFolder().toPath().resolve("data").resolve("bounties").resolve("players"));
        this.economyGateway = new BountyEconomyGateway(plugin, economy);
        this.service = new BountyService(storage, economyGateway, settings, java.time.Instant::now);
    }

    @Override
    public void onDisable() {
    }
}
```

- [ ] **Step 5: Run the wiring tests to verify they pass**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.BountyModuleWiringTest"`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyEconomyGateway.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyModule.java src/test/java/dev/linqfy/bigCasares/modules/bounties/BountyModuleWiringTest.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountySettings.java
git commit -m "feat: add bounty module wiring"
```

## Task 5: Add The Listener And Finish Module Integration

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyListener.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyProcessingResult.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyModule.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/resources/config.yml`

- [ ] **Step 1: Extend the result object so the listener can message clearly**

```java
public record BountyProcessingResult(
    double paidOutAmount,
    double newBountyAmount,
    double totalTakenAmount,
    double bleedAmount,
    boolean createdNewBounty,
    boolean paidExistingBounty
) {
    public static BountyProcessingResult empty() {
        return new BountyProcessingResult(0.0, 0.0, 0.0, 0.0, false, false);
    }
}
```

- [ ] **Step 2: Update the service to fill the new result flag**

```java
return new BountyProcessingResult(
    paidOut,
    newBounty,
    totalTaken,
    bleed,
    true,
    paidOut > 0.0
);
```

```java
return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false, paidOut > 0.0);
```

- [ ] **Step 3: Implement the listener**

```java
package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class BountyListener implements Listener {

    private final BountyService service;
    private final BountyEconomy economy;

    public BountyListener(BountyService service, BountyEconomy economy) {
        this.service = service;
        this.economy = economy;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        BountyProcessingResult result = service.handlePlayerKill(victim.getUniqueId(), killer.getUniqueId());

        if (result.paidExistingBounty()) {
            killer.sendMessage("§aCobraste " + economy.format(result.paidOutAmount()) + " por la bounty de " + victim.getName() + ".");
        }

        if (result.createdNewBounty()) {
            victim.sendMessage("§cTu muerte genero una nueva bounty de " + economy.format(result.newBountyAmount()) + ".");
            killer.sendMessage("§e" + victim.getName() + " ahora tiene una nueva bounty de " + economy.format(result.newBountyAmount()) + ".");
        } else {
            killer.sendMessage("§7" + victim.getName() + " no tenia balance suficiente para generar nueva bounty.");
        }
    }
}
```

- [ ] **Step 4: Wire the listener correctly in the module**

```java
public final class BountyModule implements PluginModule {

    private final BigCasares plugin;
    private BountyEconomyGateway economyGateway;
    private BountyService service;

    public BountyModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        Economy economy = BountyEconomyGateway.resolveOrThrow(plugin);
        BountySettings settings = new BountySettings(
            plugin.getConfig().getDouble("bounty-system.economy.take-percent", 0.30),
            plugin.getConfig().getDouble("bounty-system.economy.bounty-percent", 0.20),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-balance", 100.0),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-bounty", 25.0)
        );

        BountyStorage storage = new YamlBountyStorage(plugin.getDataFolder().toPath().resolve("data").resolve("bounties").resolve("players"));
        this.economyGateway = new BountyEconomyGateway(plugin, economy);
        this.service = new BountyService(storage, economyGateway, settings, java.time.Instant::now);
        plugin.getServer().getPluginManager().registerEvents(new BountyListener(service, economyGateway), plugin);
    }
}
```

- [ ] **Step 5: Register the module in the plugin entrypoint**

```java
import dev.linqfy.bigCasares.modules.bounties.BountyModule;
```

```java
private BountyModule bountyModule;
```

```java
this.bountyModule = new BountyModule(this);
moduleManager.register(bountyModule);
```

- [ ] **Step 6: Add the config entries**

```yml
modules:
  copper-apple: true
  mission-system: true
  bounty-system: true
```

```yml
bounty-system:
  economy:
    take-percent: 0.30
    bounty-percent: 0.20
    minimum-balance: 100.0
    minimum-bounty: 25.0
  messages:
    payout: "&aCobraste {amount} por la bounty de {victim}."
    new-bounty-victim: "&cTu muerte genero una nueva bounty de {amount}."
    new-bounty-killer: "&e{victim} ahora tiene una nueva bounty de {amount}."
    no-refresh: "&7{victim} no tenia balance suficiente para generar nueva bounty."
```

- [ ] **Step 7: Run the bounty test suite**

Run: `.\gradlew.bat test --tests "dev.linqfy.bigCasares.modules.bounties.*"`

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyListener.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyModule.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyProcessingResult.java src/main/java/dev/linqfy/bigCasares/modules/bounties/BountyService.java src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/resources/config.yml
git commit -m "feat: wire bounty module into plugin"
```

## Task 6: Run Full Verification And Review Against The Spec

**Files:**
- Review only: `docs/superpowers/specs/2026-04-19-bounty-system-design.md`
- Review only: `src/main/java/dev/linqfy/bigCasares/modules/bounties/*.java`
- Review only: `src/test/java/dev/linqfy/bigCasares/modules/bounties/*.java`

- [ ] **Step 1: Run the full test suite**

Run: `.\gradlew.bat test`

Expected: PASS with the existing mission and copper apple tests still green.

- [ ] **Step 2: Compare the implementation against the spec acceptance criteria**

```text
- PvP death pays existing bounty
- Same death refreshes new bounty from remaining balance
- New bounty persists for later kills
- Configurable 30/20/10 split defaults
- No refresh under minimum balance
- No refresh under minimum bounty
- Vault required for module enablement
```

- [ ] **Step 3: Re-read the listener and service for transaction order**

```text
1. deposit existing bounty to killer
2. read victim remaining balance
3. compute refresh amounts
4. withdraw total taken from victim
5. persist new active bounty
```

- [ ] **Step 4: Commit the final verification checkpoint**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/bounties src/test/java/dev/linqfy/bigCasares/modules/bounties src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/resources/config.yml
git commit -m "test: verify bounty system integration"
```

## Self-Review

### Spec coverage

- Module isolation: covered by Tasks 2, 4, and 5.
- Vault-only economy integration: covered by Task 4.
- YAML persistence: covered by Task 3.
- Payout plus refresh on same kill: covered by Task 1 and Task 2.
- Stacked bounty state handling: covered by Task 1 and Task 2.
- Config defaults and tuning: covered by Task 5.
- Automated verification: covered by Tasks 1, 3, 4, 5, and 6.

### Placeholder scan

- No `TODO` or `TBD` placeholders remain.
- Each code-changing step includes concrete code.
- Each verification step includes an exact Gradle command and expected outcome.

### Type consistency

- `BountySettings`, `BountyPlayerState`, `BountyProcessingResult`, and `BountyService` use the same names across all tasks.
- `BountyEconomy` is introduced before the service depends on it.
- `BountyModule` and `BountyListener` wiring matches the service constructor defined earlier.
