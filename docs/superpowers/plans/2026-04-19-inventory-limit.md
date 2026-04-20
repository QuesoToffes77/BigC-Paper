# Inventory Limit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a config-driven `inventory-limit` module that caps vanilla materials in player inventory, denies excess when possible, trims exact overflow otherwise, and revalidates shop purchases.

**Architecture:** Add a dedicated `inventory-limit` module that loads `Material -> max` limits from `config.yml`, exposes a small service for counting and trimming overflow, and registers a listener for player-inventory-only mutation events. Reuse the same service from the shop module after purchases so limits stay consistent across pickups, GUI moves, and plugin-driven grants.

**Tech Stack:** Java 21, Spigot API 1.21, JUnit 5, YAML-backed plugin config, existing `PluginModule`/reload flow.

---

### Task 1: Add failing tests for config parsing and overflow math

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitSettingsLoaderTest.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitServiceTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitSettingsLoader.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitService.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void loadsPositiveMaterialLimits() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("inventory-limit.limits.TOTEM_OF_UNDYING", 3);

    Map<Material, Integer> limits = new InventoryLimitSettingsLoader().load(config);

    assertEquals(3, limits.get(Material.TOTEM_OF_UNDYING));
}

@Test
void computesExactOverflow() {
    InventoryLimitService service = new InventoryLimitService(Map.of(Material.TOTEM_OF_UNDYING, 3));
    ItemStack[] contents = {
        new ItemStack(Material.TOTEM_OF_UNDYING, 2),
        new ItemStack(Material.TOTEM_OF_UNDYING, 3)
    };

    assertEquals(2, service.overflow(contents, Material.TOTEM_OF_UNDYING));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitSettingsLoaderTest" --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitServiceTest"`
Expected: FAIL because the inventory-limit classes do not exist yet.

- [ ] **Step 3: Add minimal class skeletons**

```java
public final class InventoryLimitSettingsLoader {
    public Map<Material, Integer> load(ConfigurationSection config) {
        throw new UnsupportedOperationException("not implemented");
    }
}
```

```java
public final class InventoryLimitService {
    public InventoryLimitService(Map<Material, Integer> limits) {
    }
}
```

- [ ] **Step 4: Run the tests again**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitSettingsLoaderTest" --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitServiceTest"`
Expected: FAIL with assertion or unsupported-operation failures, not missing symbols.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/ src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitSettingsLoader.java src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitService.java
git commit -m "test: add inventory limit parser and service coverage"
```

### Task 2: Implement parser and overflow service

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitSettingsLoader.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitService.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitSettingsLoaderTest.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitServiceTest.java`

- [ ] **Step 1: Implement config parsing for positive vanilla material limits**

```java
String materialName = limitsSection.getString(path);
Material material = Material.matchMaterial(key);
if (material == null) {
    throw new IllegalArgumentException("Material invalido: " + key);
}
if (limit <= 0) {
    throw new IllegalArgumentException("El limite debe ser mayor que cero para " + key);
}
```

- [ ] **Step 2: Implement count and overflow helpers**

```java
public int count(ItemStack[] contents, Material material) {
    int total = 0;
    for (ItemStack stack : contents) {
        if (stack != null && stack.getType() == material) {
            total += stack.getAmount();
        }
    }
    return total;
}
```

- [ ] **Step 3: Add removal test for exact overflow trimming**

```java
@Test
void trimsExactOverflow() {
    int removed = service.trimOverflow(contents, Material.TOTEM_OF_UNDYING);
    assertEquals(2, removed);
}
```

- [ ] **Step 4: Run targeted tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitSettingsLoaderTest" --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitSettingsLoader.java src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitService.java src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/
git commit -m "feat: implement inventory limit parsing and overflow math"
```

### Task 3: Add failing wiring test for the new module

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitModuleWiringTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitModule.java`

- [ ] **Step 1: Write the failing wiring test**

```java
@Test
void exposesStableModuleId() {
    assertEquals("inventory-limit", new InventoryLimitModule(null).getId());
}
```

- [ ] **Step 2: Run the wiring test**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitModuleWiringTest"`
Expected: FAIL because `InventoryLimitModule` does not exist yet.

- [ ] **Step 3: Add the minimal module skeleton**

```java
public final class InventoryLimitModule implements PluginModule {
    @Override
    public String getId() {
        return "inventory-limit";
    }
}
```

- [ ] **Step 4: Run the wiring test again**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitModuleWiringTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitModuleWiringTest.java src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitModule.java
git commit -m "test: add inventory limit module wiring"
```

### Task 4: Implement module, listener, and player revalidation API

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitModule.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/InventoryLimitListener.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/resources/config.yml`

- [ ] **Step 1: Add module registration and config defaults**

```yml
modules:
  inventory-limit: true

inventory-limit:
  limits:
    TOTEM_OF_UNDYING: 3
```

- [ ] **Step 2: Load limits and register the listener**

```java
this.service = new InventoryLimitService(new InventoryLimitSettingsLoader().load(plugin.getConfig()));
plugin.getServer().getPluginManager().registerEvents(new InventoryLimitListener(this, service), plugin);
```

- [ ] **Step 3: Expose a public revalidation method for plugin-driven item grants**

```java
public int enforce(Player player) {
    return service.enforcePlayerInventory(player);
}
```

- [ ] **Step 4: Handle deny-first events and fallback trim/drop**

```java
if (service.canAccept(player, material, incomingAmount)) {
    return;
}
event.setCancelled(true);
```

- [ ] **Step 5: Run inventory-limit test suite**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.*"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/inventorylimit/ src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/resources/config.yml src/test/java/dev/linqfy/bigCasares/modules/inventorylimit/
git commit -m "feat: add inventory limit module"
```

### Task 5: Integrate shop purchase enforcement

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopService.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopServiceTest.java`

- [ ] **Step 1: Add failing test for post-purchase enforcement hook**

```java
@Test
void buyCanReportOverflowAfterGrant() {
    ShopTransactionResult result = service.buy(entry);
    assertEquals(2, result.overflowRemoved());
}
```

- [ ] **Step 2: Run the targeted shop test**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopServiceTest"`
Expected: FAIL because the overflow enforcement integration does not exist yet.

- [ ] **Step 3: Inject an optional inventory-limit callback into shop buy flow**

```java
int overflowRemoved = postGrantEnforcer.applyAsInt(player);
return ShopTransactionResult.success(message, purchased, overflowRemoved);
```

- [ ] **Step 4: Run the targeted shop and inventory-limit tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopServiceTest" --tests "dev.linqfy.bigCasares.modules.inventorylimit.*"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/shop/ShopService.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java src/test/java/dev/linqfy/bigCasares/modules/shop/ShopServiceTest.java
git commit -m "feat: enforce inventory limits after shop purchases"
```

### Task 6: Final verification

**Files:**
- Verify: `src/main/java/dev/linqfy/bigCasares/**`
- Verify: `src/test/java/dev/linqfy/bigCasares/**`
- Verify: `src/main/resources/config.yml`
- Verify: `docs/superpowers/specs/2026-04-19-inventory-limit-design.md`
- Verify: `docs/superpowers/plans/2026-04-19-inventory-limit.md`

- [ ] **Step 1: Run targeted tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.inventorylimit.*" --tests "dev.linqfy.bigCasares.modules.shop.ShopServiceTest"`
Expected: PASS

- [ ] **Step 2: Run broader regression tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.missions.*" --tests "dev.linqfy.bigCasares.modules.bounties.*" --tests "dev.linqfy.bigCasares.modules.shop.*" --tests "dev.linqfy.bigCasares.modules.inventorylimit.*" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: PASS

- [ ] **Step 3: Run full suite**

Run: `gradle test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares src/test/java/dev/linqfy/bigCasares src/main/resources/config.yml docs/superpowers/specs/2026-04-19-inventory-limit-design.md docs/superpowers/plans/2026-04-19-inventory-limit.md
git commit -m "feat: add config-driven inventory limits"
```
