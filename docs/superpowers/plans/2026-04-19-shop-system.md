# Shop System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a `shop-system` module with fixed container GUIs, `shop.yml`-driven categories and entries, support for vanilla and custom registry items, and `/bigcasares reload` hot reload support.

**Architecture:** Add a dedicated `shop-system` module that loads an immutable in-memory catalog from `shop.yml`, resolves item sources through a shared resolver, and handles buy/sell actions through a small domain service. Refactor command ownership into a central router so `/bigcasares` can delegate to `give`, `misiones`, `shop`, and `reload` without depending on `CopperAppleModule`.

**Tech Stack:** Java 21, Spigot API 1.21, Vault API, JUnit 5, YAML-backed plugin configs.

---

### Task 1: Add failing tests for shop catalog parsing

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopCatalogLoaderTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCatalogLoader.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCatalog.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCategory.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopEntry.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void loadsCategoryAndBothItemSources() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("categories.blocks.name", "&6Bloques");
    config.set("categories.blocks.icon", "STONE");
    config.set("categories.blocks.slot", 10);
    config.set("categories.blocks.items.stone.slot", 10);
    config.set("categories.blocks.items.stone.material", "STONE");
    config.set("categories.blocks.items.stone.amount", 64);
    config.set("categories.blocks.items.stone.buy-price", 64.0);
    config.set("categories.blocks.items.stone.sell-price", 4.0);
    config.set("categories.blocks.items.apple.slot", 12);
    config.set("categories.blocks.items.apple.custom-item-id", "copper_apple");
    config.set("categories.blocks.items.apple.amount", 1);
    config.set("categories.blocks.items.apple.buy-price", 1500.0);
    config.set("categories.blocks.items.apple.sell-price", 50.0);

    ShopCatalog catalog = new ShopCatalogLoader().load(config);

    assertEquals(1, catalog.categories().size());
    assertEquals(2, catalog.category("blocks").orElseThrow().entries().size());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopCatalogLoaderTest"`
Expected: FAIL because shop classes do not exist yet.

- [ ] **Step 3: Write minimal implementation**

```java
public final class ShopCatalogLoader {
    public ShopCatalog load(ConfigurationSection root) {
        throw new UnsupportedOperationException("not implemented");
    }
}
```

- [ ] **Step 4: Run test to verify it still fails for behavior, not missing symbols**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopCatalogLoaderTest"`
Expected: FAIL with unsupported operation or assertion failure.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/modules/shop/ShopCatalogLoaderTest.java src/main/java/dev/linqfy/bigCasares/modules/shop/
git commit -m "test: add shop catalog loader coverage"
```

### Task 2: Implement catalog parsing and validation

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCatalogLoader.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCatalog.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCategory.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopEntry.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopCatalogLoaderTest.java`

- [ ] **Step 1: Implement parsing for categories and entries**

```java
String materialName = section.getString("material");
String customItemId = section.getString("custom-item-id");
if ((materialName == null) == (customItemId == null)) {
    throw new IllegalArgumentException("Entry must define exactly one item source.");
}
```

- [ ] **Step 2: Add validation tests for invalid sources and slots**

```java
@Test
void rejectsEntryWithBothItemSources() {
    assertThrows(IllegalArgumentException.class, () -> new ShopCatalogLoader().load(config));
}
```

- [ ] **Step 3: Run targeted tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopCatalogLoaderTest"`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCatalog*.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopCategory.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopEntry.java src/test/java/dev/linqfy/bigCasares/modules/shop/ShopCatalogLoaderTest.java
git commit -m "feat: parse shop catalog from yaml"
```

### Task 3: Add failing tests for item resolution and selling behavior

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopServiceTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopItemResolver.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopTransactionResult.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/items/CustomItemRegistry.java`

- [ ] **Step 1: Write failing tests for vanilla buy and custom sell matching**

```java
@Test
void buysVanillaEntryWhenBalanceIsEnough() {
    ShopTransactionResult result = service.buy(player, vanillaEntry);
    assertTrue(result.success());
}

@Test
void sellsCustomEntryOnlyWhenMatchingRegistryItemExists() {
    ShopTransactionResult result = service.sell(player, customEntry);
    assertTrue(result.success());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopServiceTest"`
Expected: FAIL because service and resolver behavior are missing.

- [ ] **Step 3: Add minimal supporting APIs**

```java
public record ShopTransactionResult(boolean success, String message) {
}
```

- [ ] **Step 4: Run tests again**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopServiceTest"`
Expected: FAIL with behavioral assertions, not missing classes.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/modules/shop/ShopServiceTest.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopService.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopItemResolver.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopTransactionResult.java src/main/java/dev/linqfy/bigCasares/items/CustomItemRegistry.java
git commit -m "test: add shop service coverage"
```

### Task 4: Implement resolver and domain service

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopItemResolver.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopService.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopEconomyGateway.java`
- Test: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopServiceTest.java`

- [ ] **Step 1: Implement entry resolution for material and custom item ids**

```java
public ItemStack createStack(ShopEntry entry) {
    if (entry.material() != null) {
        return new ItemStack(entry.material(), entry.amount());
    }
    CustomItem item = registry.findById(entry.customItemId()).orElseThrow();
    return item.createItemStack(entry.amount());
}
```

- [ ] **Step 2: Implement buy flow**

```java
if (!economy.has(player, entry.buyPrice())) {
    return ShopTransactionResult.failure("Saldo insuficiente.");
}
```

- [ ] **Step 3: Implement sell flow using inventory scan and exact custom matching**

```java
if (entry.customItemId() != null) {
    return removeMatchingCustomStacks(player.getInventory(), entry);
}
return removeMatchingMaterialStacks(player.getInventory(), entry);
```

- [ ] **Step 4: Run targeted tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopServiceTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/shop/ShopItemResolver.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopService.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopEconomyGateway.java src/test/java/dev/linqfy/bigCasares/modules/shop/ShopServiceTest.java
git commit -m "feat: implement shop buy and sell service"
```

### Task 5: Add failing tests for command routing and reload behavior

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java`
- Create: `src/main/java/dev/linqfy/bigCasares/command/CommandCompletionHelper.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleModule.java`

- [ ] **Step 1: Write failing tests for `/bigcasares reload` and `/bigcasares shop` parsing**

```java
@Test
void recognizesReloadSubcommand() {
    assertTrue(BigCasaresCommand.isReload("reload"));
}
```

- [ ] **Step 2: Run targeted test**

Run: `gradle test --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: FAIL because command router does not exist yet.

- [ ] **Step 3: Add minimal command skeleton**

```java
public final class BigCasaresCommand implements CommandExecutor, TabCompleter {
}
```

- [ ] **Step 4: Run targeted test again**

Run: `gradle test --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: FAIL with assertion-level failure.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java src/main/java/dev/linqfy/bigCasares/command/CommandCompletionHelper.java src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleModule.java
git commit -m "test: cover main command routing"
```

### Task 6: Implement command router, `/shop`, and reload hooks

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleModule.java`
- Modify: `src/main/resources/plugin.yml`
- Test: `src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java`

- [ ] **Step 1: Register the shared `/bigcasares` executor from plugin startup**

```java
PluginCommand command = getCommand("bigcasares");
command.setExecutor(new BigCasaresCommand(this));
command.setTabCompleter(new BigCasaresCommand(this));
```

- [ ] **Step 2: Add `/shop` to `plugin.yml` and route it to the shop module**

```yml
shop:
  description: Open the server shop
  usage: /<command>
```

- [ ] **Step 3: Implement `/bigcasares reload`**

```java
plugin.reloadConfig();
plugin.reloadModules();
sender.sendMessage(ChatColor.GREEN + "Configs recargadas.");
```

- [ ] **Step 4: Run targeted command tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/command/BigCasaresCommand.java src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/java/dev/linqfy/bigCasares/modules/copperapple/CopperAppleModule.java src/main/resources/plugin.yml src/test/java/dev/linqfy/bigCasares/command/BigCasaresCommandTest.java
git commit -m "feat: centralize commands and add reload"
```

### Task 7: Add failing tests for shop module wiring

**Files:**
- Create: `src/test/java/dev/linqfy/bigCasares/modules/shop/ShopModuleWiringTest.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java`

- [ ] **Step 1: Write failing wiring test**

```java
@Test
void exposesStableModuleId() {
    assertEquals("shop-system", new ShopModule(null).getId());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopModuleWiringTest"`
Expected: FAIL because `ShopModule` does not exist yet.

- [ ] **Step 3: Add minimal module skeleton**

```java
public final class ShopModule implements PluginModule {
    @Override
    public String getId() {
        return "shop-system";
    }
}
```

- [ ] **Step 4: Run targeted test**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.ShopModuleWiringTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/dev/linqfy/bigCasares/modules/shop/ShopModuleWiringTest.java src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java
git commit -m "test: add shop module wiring"
```

### Task 8: Implement shop module, GUI controller, and default config

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopModule.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/shop/ShopGuiController.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/resources/config.yml`
- Create: `src/main/resources/shop.yml`

- [ ] **Step 1: Add `shop-system` enablement and module registration**

```java
this.shopModule = new ShopModule(this);
moduleManager.register(shopModule);
```

- [ ] **Step 2: Save and load `shop.yml` on enable**

```java
plugin.saveResource("shop.yml", false);
YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
```

- [ ] **Step 3: Implement fixed category and item GUIs**

```java
Inventory inventory = Bukkit.createInventory(holder, 27, CATEGORY_TITLE);
Inventory inventory = Bukkit.createInventory(holder, 54, itemTitle(category));
```

- [ ] **Step 4: Handle left-click buy, right-click sell, and back button navigation**

```java
if (event.isLeftClick()) {
    service.buy(player, entry);
} else if (event.isRightClick()) {
    service.sell(player, entry);
}
```

- [ ] **Step 5: Run shop test suite**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.*" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares/modules/shop/ src/main/java/dev/linqfy/bigCasares/BigCasares.java src/main/resources/config.yml src/main/resources/shop.yml
git commit -m "feat: add shop module and guis"
```

### Task 9: Final verification

**Files:**
- Verify: `src/main/java/dev/linqfy/bigCasares/**`
- Verify: `src/test/java/dev/linqfy/bigCasares/**`
- Verify: `src/main/resources/plugin.yml`
- Verify: `src/main/resources/shop.yml`

- [ ] **Step 1: Run targeted tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.shop.*" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: PASS

- [ ] **Step 2: Run broader regression tests**

Run: `gradle test --tests "dev.linqfy.bigCasares.modules.missions.*" --tests "dev.linqfy.bigCasares.modules.bounties.*" --tests "dev.linqfy.bigCasares.modules.shop.*" --tests "dev.linqfy.bigCasares.command.BigCasaresCommandTest"`
Expected: PASS

- [ ] **Step 3: Run full build verification**

Run: `gradle test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add src/main/java/dev/linqfy/bigCasares src/test/java/dev/linqfy/bigCasares src/main/resources/plugin.yml src/main/resources/config.yml src/main/resources/shop.yml docs/superpowers/specs/2026-04-19-shop-system-design.md docs/superpowers/plans/2026-04-19-shop-system.md
git commit -m "feat: add configurable shop system"
```
