# Nuke Rings and Vanilla Textures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the Nuke Shot from one center TNT into five exponentially populated rings totaling exactly 200 TNT, while reusing vanilla Minecraft TNT and compass assets.

**Architecture:** Keep geometry in the Bukkit-free `NukeRingLayout`, using normalized powers-of-two weights and largest-remainder allocation. Keep the center owned by `NukeAnimationRuntime`, retain it throughout ring expansion, and include it in the atomic release. Point Java item definitions and the shared cross-platform registry at built-in Minecraft assets so existing custom PNGs are no longer selected.

**Tech Stack:** Java 21, Paper API, JUnit 5, Gradle 9.2.1, Minecraft 1.21 resource-pack JSON, BigCasares shared Java/Bedrock resource-pack assembler

---

## File Structure

- Modify `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayout.java`: normalized exponential ring allocation.
- Modify `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationRuntime.java`: retain and release the center TNT.
- Modify `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java`: defaults and validation for five rings and 200 total TNT.
- Modify `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayoutTest.java`: exact geometry and invalid-input behavior.
- Modify `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationTimelineTest.java`: five expansion ticks and tick-25 release.
- Modify `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettingsTest.java`: new defaults and exponential minimum validation.
- Modify `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsResourcePackTest.java`: built-in Java and Bedrock asset references.
- Modify `src/main/resources/config.yml`: five rings and 200 TNT.
- Modify `src/main/resources/content/items/nuke_shot.yml`: update the 200-TNT lore.
- Modify `resourcepack/shared/registry.yml`: select vanilla Bedrock texture atlas paths while retaining the custom item-definition IDs required by the assembler.
- Modify `resourcepack/java/assets/bigcasares/models/item/{nuke_shot,tracker_compass}.json`: inherit built-in Java models.
- Modify `resourcepack/assets/bigcasares/models/item/{nuke_shot,tracker_compass}.json`: mirrored compatibility models.
- Modify `resourcepack/bedrock/textures/item_texture.json`: checked-in Bedrock atlas references to vanilla textures.

### Task 1: Exponential 200-TNT Geometry

**Files:**
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayoutTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayout.java`

- [ ] **Step 1: Write the failing exact-population test**

Replace the old 130-point expectation with:

```java
@Test
void buildsFiveExponentiallyGrowingRingsAroundOneCenterForTwoHundredTotalTnt() {
    List<NukeRing> rings = NukeRingLayout.create(5, 199, 2.0);

    assertEquals(5, rings.size());
    assertEquals(List.of(6, 13, 26, 51, 103),
        rings.stream().map(ring -> ring.points().size()).toList());
    assertEquals(199, rings.stream().mapToInt(ring -> ring.points().size()).sum());
}
```

Retain the unique-coordinate and angular-spacing assertions, changing their inputs to `create(5, 199, 2.0)` and expected unique ring points to `199`. Replace the impossible-total test with `create(5, 30, 2.0)`, because five exponential weights require at least `1 + 2 + 4 + 8 + 16 = 31` ring points.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.NukeRingLayoutTest" --no-daemon
```

Expected: FAIL because the current allocator returns a linear population instead of `6, 13, 26, 51, 103` and accepts totals below the exponential minimum.

- [ ] **Step 3: Implement normalized doubling with largest remainders**

Replace `populations` with allocation based on weights `1L << index`. Validate `ringCount <= 30` and `totalPoints >= (1L << ringCount) - 1L`. Floor each exact quota, record its remainder, then award the remaining points in descending remainder order (outer index wins an exact tie):

```java
private static int[] populations(int ringCount, int totalPoints) {
    long weightTotal = (1L << ringCount) - 1L;
    if (totalPoints < weightTotal) {
        throw new IllegalArgumentException("total points cannot form exponential rings");
    }

    int[] values = new int[ringCount];
    long[] remainders = new long[ringCount];
    int assigned = 0;
    for (int index = 0; index < ringCount; index++) {
        long weighted = (long) totalPoints * (1L << index);
        values[index] = (int) (weighted / weightTotal);
        remainders[index] = weighted % weightTotal;
        assigned += values[index];
    }
    while (assigned < totalPoints) {
        int winner = 0;
        for (int index = 1; index < ringCount; index++) {
            if (remainders[index] >= remainders[winner]) winner = index;
        }
        values[winner]++;
        remainders[winner] = -1L;
        assigned++;
    }
    return values;
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Task 1 Gradle command again. Expected: PASS.

- [ ] **Step 5: Commit the geometry slice**

```powershell
git add -- src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayout.java src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayoutTest.java
git commit -m "fix: build exponential nuke rings"
```

### Task 2: Center Retention, Timing, and Defaults

**Files:**
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationTimelineTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettingsTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationRuntime.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/content/items/nuke_shot.yml`

- [ ] **Step 1: Write failing timeline and settings tests**

Change the timeline expectation to:

```java
@Test
void startsWithCenterBuildsFiveRingsAndReleasesAtTickTwentyFive() {
    NukeAnimationTimeline timeline = new NukeAnimationTimeline(5, 5L);

    assertEquals(0L, timeline.seedTick());
    assertEquals(List.of(5L, 10L, 15L, 20L, 25L), timeline.ringTicks());
    assertEquals(25L, timeline.releaseTick());
}
```

Change default settings assertions to `ringCount() == 5` and `totalTnt() == 200`. Change the invalid-total constructor case to five rings and total `31`, which is one below the required center plus 31 ring points:

```java
assertThrows(IllegalArgumentException.class,
    () -> new SpecialItemsSettings(45_000L, 60_000L, 20L, 50.0, 5, 31, 5L, 2.0));
```

- [ ] **Step 2: Run tests and verify RED**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.NukeAnimationTimelineTest" --tests "dev.linqfy.bigCasares.modules.specialitems.SpecialItemsSettingsTest" --no-daemon
```

Expected: settings test FAIL because defaults remain ten rings and 130 TNT.

- [ ] **Step 3: Implement settings and runtime center behavior**

In `SpecialItemsSettings`, calculate the minimum as one center plus the exponential weight sum:

```java
long minimumTnt = ringCount >= 31 ? Long.MAX_VALUE : 1L << ringCount;
```

Reject `ringCount > 30` or `totalTnt < minimumTnt`. Load defaults `ring-count: 5` and `total-tnt: 200`.

In `NukeAnimationRuntime`, pass only non-center points to the layout:

```java
this.layout = NukeRingLayout.create(
    settings.ringCount(), settings.totalTnt() - 1, settings.radiusStep());
```

When starting an animation, retain the center for release:

```java
animation.displays.add(spawnDisplay(center));
animation.tntLocations.add(center.clone());
```

Remove the `animation.nextRing == 0` block that deletes the center display. Do not change release cleanup, so it converts the retained center and all ring locations together.

Update `config.yml` to five rings and 200 total TNT. Change Nuke Shot lore to `§cUn disparo. Doscientos problemas.`

- [ ] **Step 4: Run all special-items tests and verify GREEN**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.*" --no-daemon
```

Expected: PASS.

- [ ] **Step 5: Commit runtime and defaults**

```powershell
git add -- src/main/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationRuntime.java src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationTimelineTest.java src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettingsTest.java src/main/resources/config.yml src/main/resources/content/items/nuke_shot.yml
git commit -m "fix: retain center in 200 tnt nuke"
```

### Task 3: Vanilla Minecraft Item Assets

**Files:**
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsResourcePackTest.java`
- Modify: `resourcepack/shared/registry.yml`
- Modify: `resourcepack/java/assets/bigcasares/models/item/nuke_shot.json`
- Modify: `resourcepack/java/assets/bigcasares/models/item/tracker_compass.json`
- Modify: `resourcepack/assets/bigcasares/models/item/nuke_shot.json`
- Modify: `resourcepack/assets/bigcasares/models/item/tracker_compass.json`
- Modify: `resourcepack/bedrock/textures/item_texture.json`

- [ ] **Step 1: Write the failing vanilla-reference test**

Replace the dedicated custom-texture assertions with exact model/registry checks:

```java
@Test
void reusesVanillaMinecraftModelsAndTextures() throws Exception {
    assertContains(PACK.resolve("java/assets/bigcasares/models/item/nuke_shot.json"),
        "minecraft:block/tnt");
    assertContains(PACK.resolve("java/assets/bigcasares/models/item/tracker_compass.json"),
        "minecraft:item/compass");
    assertContains(PACK.resolve("assets/bigcasares/models/item/nuke_shot.json"),
        "minecraft:block/tnt");
    assertContains(PACK.resolve("assets/bigcasares/models/item/tracker_compass.json"),
        "minecraft:item/compass");
    assertContains(PACK.resolve("shared/registry.yml"),
        "java-model: bigcasares:item/nuke_shot", "texture: blocks/tnt_side",
        "java-model: bigcasares:item/tracker_compass", "texture: items/compass_item");
    assertContains(PACK.resolve("bedrock/textures/item_texture.json"),
        "textures/blocks/tnt_side", "textures/items/compass_item");
}
```

Keep language and catalog registry assertions. Remove `ImageIO`, `BufferedImage`, `assertTexture`, and `assertNotEquals` imports/code from this test.

- [ ] **Step 2: Run the resource test and verify RED**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.SpecialItemsResourcePackTest" --no-daemon
```

Expected: FAIL because item definitions and registry still reference `bigcasares` custom assets.

- [ ] **Step 3: Point pack metadata at built-in assets**

Set both Nuke Shot local model files to:

```json
{
  "parent": "minecraft:block/tnt"
}
```

Set both Tracker Compass local model files identically except for `"parent": "minecraft:item/compass"`.

In `resourcepack/shared/registry.yml`, set tracker to:

```yaml
  tracker-compass:
    type: item-model
    java-model: bigcasares:item/tracker_compass
    texture: items/compass_item
```

Set nuke to:

```yaml
  nuke-shot:
    type: item-model
    java-model: bigcasares:item/nuke_shot
    texture: blocks/tnt_side
```

Update checked-in `resourcepack/bedrock/textures/item_texture.json` so `bigcasares.tracker_compass` maps to `textures/items/compass_item` and `bigcasares.nuke_shot` maps to `textures/blocks/tnt_side`. Leave existing PNG files untouched but unused; no texture is downloaded, generated, or overwritten.

- [ ] **Step 4: Run resource and catalog tests and verify GREEN**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.SpecialItemsResourcePackTest" --tests "dev.linqfy.bigCasares.items.catalog.*" --no-daemon
```

Expected: PASS.

- [ ] **Step 5: Commit vanilla asset references**

```powershell
git add -- src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsResourcePackTest.java resourcepack/shared/registry.yml resourcepack/java/assets/bigcasares/models/item/nuke_shot.json resourcepack/java/assets/bigcasares/models/item/tracker_compass.json resourcepack/assets/bigcasares/models/item/nuke_shot.json resourcepack/assets/bigcasares/models/item/tracker_compass.json resourcepack/bedrock/textures/item_texture.json
git commit -m "fix: reuse vanilla nuke and compass assets"
```

### Task 4: Regression Verification

**Files:**
- Verify only; do not run `gradle clean`.

- [ ] **Step 1: Run the focused special-items suite**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.*" --no-daemon
```

Expected: BUILD SUCCESSFUL with all special-items tests passing.

- [ ] **Step 2: Run the complete test suite**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Build all artifacts without cleaning runtime state**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle build --no-daemon
```

Expected: BUILD SUCCESSFUL and `build/run-server` remains present.

- [ ] **Step 4: Inspect only the task-owned diff**

```powershell
git diff --check
git status --short
```

Expected: no whitespace errors; unrelated pre-existing changes remain untouched.

### Task 5: Six Rings, 350 TNT, and Three-Block Spacing

**Files:**
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayoutTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationTimelineTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettingsTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/items/catalog/DefaultItemCatalogTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/content/items/nuke_shot.yml`

- [ ] **Step 1: Write failing final-formation tests**

Change the geometry assertions to the six-ring target:

```java
@Test
void buildsSixExponentiallyGrowingRingsAroundOneCenterForThreeHundredFiftyTotalTnt() {
    List<NukeRing> rings = NukeRingLayout.create(6, 349, 3.0);

    assertEquals(6, rings.size());
    assertEquals(List.of(6, 11, 22, 44, 89, 177),
        rings.stream().map(ring -> ring.points().size()).toList());
    assertEquals(349, rings.stream().mapToInt(ring -> ring.points().size()).sum());

    Set<String> coordinates = new HashSet<>();
    rings.stream().flatMap(ring -> ring.points().stream()).forEach(point ->
        coordinates.add("%.6f:%.6f".formatted(point.x(), point.z())));
    assertEquals(349, coordinates.size());
}
```

Change the spacing test to `NukeRingLayout.create(6, 349, 3.0)` and assert `(index + 1) * 3.0`, producing radii `3`, `6`, `9`, `12`, `15`, and `18`.

Change the timeline test to:

```java
@Test
void startsWithCenterBuildsSixRingsAndReleasesAtTickThirty() {
    NukeAnimationTimeline timeline = new NukeAnimationTimeline(6, 5L);

    assertEquals(0L, timeline.seedTick());
    assertEquals(List.of(5L, 10L, 15L, 20L, 25L, 30L), timeline.ringTicks());
    assertEquals(30L, timeline.releaseTick());
}
```

In `SpecialItemsSettingsTest.loadsConfirmedDefaults`, assert:

```java
assertEquals(6, settings.ringCount());
assertEquals(350, settings.totalTnt());
assertEquals(3.0, settings.radiusStep());
```

In `DefaultItemCatalogTest`, assert the updated lore:

```java
assertEquals(List.of("§cUn disparo. Trescientos cincuenta problemas."),
    catalog.require("nuke_shot").display().lore());
```

- [ ] **Step 2: Run the focused tests and verify RED**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.NukeRingLayoutTest" --tests "dev.linqfy.bigCasares.modules.specialitems.NukeAnimationTimelineTest" --tests "dev.linqfy.bigCasares.modules.specialitems.SpecialItemsSettingsTest" --tests "dev.linqfy.bigCasares.items.catalog.DefaultItemCatalogTest" --no-daemon
```

Expected: settings defaults FAIL because they remain `5/200/10.0`, and catalog lore FAILS because it still says `Doscientos`. Geometry and timeline calculations document the generic layout/timeline behavior for the new settings.

- [ ] **Step 3: Change the runtime defaults and user-facing lore**

In `SpecialItemsSettings.load`, set:

```java
config.getInt("special-items.nuke-shot.ring-count", 6),
config.getInt("special-items.nuke-shot.total-tnt", 350),
config.getLong("special-items.nuke-shot.ring-interval-ticks", 5L),
config.getDouble("special-items.nuke-shot.radius-step", 3.0)
```

Change only the nuke values in `src/main/resources/config.yml`:

```yaml
special-items:
  nuke-shot:
    ring-count: 6
    total-tnt: 350
    ring-interval-ticks: 5
    radius-step: 3.0
```

Change the Nuke Shot lore in `src/main/resources/content/items/nuke_shot.yml` to:

```yaml
lore:
  - "§cUn disparo. Trescientos cincuenta problemas."
```

No runtime algorithm change is needed: `NukeAnimationRuntime` already consumes `ringCount`, `totalTnt - 1`, and `radiusStep`, while `NukeRingLayout` already normalizes the exponential populations.

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the Step 2 command again. Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run the full special-items and catalog suites**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.*" --tests "dev.linqfy.bigCasares.items.catalog.*" --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Verify packaging without cleaning**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle build -x test --no-daemon
```

Expected: BUILD SUCCESSFUL; generated resource packs refresh and `build/run-server` remains intact.
