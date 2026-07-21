# Nuke 350 TNT Six-Ring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Configure the Nuke Shot as one center TNT plus six exponentially populated rings totaling exactly 350 TNT, spaced three blocks apart.

**Architecture:** Reuse the existing generic `NukeRingLayout` normalized-doubling allocator and `NukeAnimationTimeline`. Change only configuration defaults, tests, and user-facing lore; the runtime already consumes `ringCount`, `totalTnt - 1`, and `radiusStep`.

**Tech Stack:** Java 21, Paper API, JUnit 5, Gradle 9.2.1, Bukkit YAML configuration

---

### Task 1: Six Rings, 350 TNT, and Three-Block Spacing

**Files:**
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeRingLayoutTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/NukeAnimationTimelineTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettingsTest.java`
- Modify: `src/test/java/dev/linqfy/bigCasares/items/catalog/DefaultItemCatalogTest.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/specialitems/SpecialItemsSettings.java`
- Modify: `src/main/resources/config.yml`
- Modify: `src/main/resources/content/items/nuke_shot.yml`

- [ ] **Step 1: Write failing final-formation tests**

Set the geometry test to:

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

Set the spacing test input to `NukeRingLayout.create(6, 349, 3.0)` and assert `(index + 1) * 3.0`, producing radii `3`, `6`, `9`, `12`, `15`, and `18`.

Set the timeline test to:

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

In `DefaultItemCatalogTest`, assert:

```java
assertEquals(List.of("§cUn disparo. Trescientos cincuenta problemas."),
    catalog.require("nuke_shot").display().lore());
```

- [ ] **Step 2: Run the focused tests and verify RED**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.NukeRingLayoutTest" --tests "dev.linqfy.bigCasares.modules.specialitems.NukeAnimationTimelineTest" --tests "dev.linqfy.bigCasares.modules.specialitems.SpecialItemsSettingsTest" --tests "dev.linqfy.bigCasares.items.catalog.DefaultItemCatalogTest" --no-daemon
```

Expected: settings defaults FAIL because they are not yet `6/350/3.0`, and catalog lore FAILS because it does not yet say `Trescientos cincuenta`.

- [ ] **Step 3: Change defaults and lore**

In `SpecialItemsSettings.load`, set:

```java
config.getInt("special-items.nuke-shot.ring-count", 6),
config.getInt("special-items.nuke-shot.total-tnt", 350),
config.getLong("special-items.nuke-shot.ring-interval-ticks", 5L),
config.getDouble("special-items.nuke-shot.radius-step", 3.0)
```

Set only these nuke keys in `src/main/resources/config.yml`:

```yaml
special-items:
  nuke-shot:
    ring-count: 6
    total-tnt: 350
    ring-interval-ticks: 5
    radius-step: 3.0
```

Set the lore in `src/main/resources/content/items/nuke_shot.yml`:

```yaml
lore:
  - "§cUn disparo. Trescientos cincuenta problemas."
```

- [ ] **Step 4: Run the focused tests and verify GREEN**

Run the Step 2 command again. Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run related regression suites**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle test --tests "dev.linqfy.bigCasares.modules.specialitems.*" --tests "dev.linqfy.bigCasares.items.catalog.*" --no-daemon
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Package without cleaning runtime state**

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
gradle build -x test --no-daemon
```

Expected: BUILD SUCCESSFUL and `build/run-server` remains present.
