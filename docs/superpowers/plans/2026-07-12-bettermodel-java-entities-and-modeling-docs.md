# BetterModel Java Entity Rendering and Modeling Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the Nexus and Abyss Guardian through BetterModel on Java, preserve native Bedrock presentation, and document Blockbench workflows for placeable entities, bosses, and player animations.

**Architecture:** BigCasares gets one internal `JavaModelGateway` that wraps BetterModel trackers. The Nexus Ghast and boss Warden remain authoritative gameplay entities; BetterModel owns their Java display bones. Bedrock keeps the existing native geometry, animations, and controllers. A merged Java pack contains both BigCasares and BetterModel assets.

**Tech Stack:** Java 25, Paper 26.2, BetterModel Bukkit API 3.2.0, Blockbench, JUnit 5, Geyser, Gradle.

---

### Task 1: Add BetterModel as a required runtime dependency

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/plugin.yml`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/model/BetterModelDependencyContractTest.java`

- [ ] **Step 1: Write the failing descriptor test**

```java
@Test
void pluginDescriptorRequiresBetterModel() throws Exception {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
        assertNotNull(input);
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(input, StandardCharsets.UTF_8));
        assertEquals(List.of("BetterModel"), yaml.getStringList("depend"));
    }
}
```

- [ ] **Step 2: Run the test and confirm it fails because `depend` is missing**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.model.BetterModelDependencyContractTest`

Expected: assertion failure showing an empty dependency list.

- [ ] **Step 3: Declare the released API and required plugin**

```groovy
// build.gradle
compileOnly("io.github.toxicity188:bettermodel-bukkit-api:3.2.0")
```

```yaml
# plugin.yml top level
depend: [BetterModel]
```

- [ ] **Step 4: Re-run the focused test**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.model.BetterModelDependencyContractTest`

Expected: `BUILD SUCCESSFUL`.

### Task 2: Build a testable BetterModel boundary

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/model/JavaModelHandle.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/model/JavaModelGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/model/JavaModelKeys.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/model/BetterModelJavaModelGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/model/JavaModelGatewayFactory.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/model/BetterModelJavaModelGatewayTest.java`

- [ ] **Step 1: Write the failing gateway lifecycle tests**

```java
@Test
void attachRecordsTheAnchorAndStableModelKey() {
    var port = new RecordingTrackerPort();
    JavaModelGateway gateway = new BetterModelJavaModelGateway(port);

    JavaModelHandle handle = gateway.attach(anchor, JavaModelKeys.NEXUS);

    assertEquals(anchor.getUniqueId(), handle.anchorId());
    assertEquals(List.of("attach:bigcasares_nexus"), port.events());
}

@Test
void closeStopsTheTrackerExactlyOnce() {
    var port = new RecordingTrackerPort();
    JavaModelGateway gateway = new BetterModelJavaModelGateway(port);
    JavaModelHandle handle = gateway.attach(anchor, JavaModelKeys.NEXUS);

    gateway.close(handle);
    gateway.close(handle);

    assertEquals(List.of("attach:bigcasares_nexus", "close:bigcasares_nexus"), port.events());
}
```

- [ ] **Step 2: Run the test and confirm it fails because the gateway is absent**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.model.BetterModelJavaModelGatewayTest`

Expected: test compilation failure referencing `BetterModelJavaModelGateway`.

- [ ] **Step 3: Implement the smallest adapter contract**

```java
public record JavaModelHandle(UUID anchorId, String modelKey) {}

public interface JavaModelGateway {
    JavaModelHandle attach(Entity anchor, String modelKey);
    boolean animate(JavaModelHandle handle, String animationKey);
    void close(JavaModelHandle handle);
}
```

The production tracker port must use BetterModel 3.2.0:

```java
EntityTracker tracker = BetterModel.model(modelKey)
    .map(renderer -> renderer.getOrCreate(BukkitAdapter.adapt(anchor)))
    .orElseThrow(() -> new IllegalStateException("BetterModel model not loaded: " + modelKey));
boolean started = tracker.animate(animationKey);
tracker.close();
```

`JavaModelGatewayFactory.create(JavaPlugin)` must reject a disabled BetterModel plugin with `BetterModel is required. Install BetterModel 3.2.0 before starting BigCasares.` Define `NEXUS = "bigcasares_nexus"` and `ABYSS_GUARDIAN = "bigcasares_abyss_guardian"` in `JavaModelKeys`.

- [ ] **Step 4: Re-run the adapter tests**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.model.BetterModelJavaModelGatewayTest`

Expected: `BUILD SUCCESSFUL`.

### Task 3: Inject one gateway into both gameplay modules

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModule.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java`
- Create: `src/test/java/dev/linqfy/bigCasares/BetterModelModuleWiringTest.java`

- [ ] **Step 1: Write the failing constructor contract test**

```java
@Test
void nexusAndBossModulesAcceptTheSharedModelGateway() {
    JavaModelGateway models = new RecordingJavaModelGateway();

    assertDoesNotThrow(() -> new NexusModule(plugin, playerTeams, teamNames, models));
    assertDoesNotThrow(() -> new PveBossModule(plugin, platforms, resourcePackLoaded, models));
}
```

- [ ] **Step 2: Run it and confirm it fails because the constructors lack the gateway**

Run: `gradle test --tests dev.linqfy.bigCasares.BetterModelModuleWiringTest`

Expected: test compilation failure for both constructors.

- [ ] **Step 3: Create and pass the shared adapter**

```java
JavaModelGateway javaModels = JavaModelGatewayFactory.create(this);
this.nexusModule = new NexusModule(this, playerTeams, teamNames, javaModels);
this.pveBossModule = new PveBossModule(this, platforms, resourcePackLoaded, javaModels);
```

Keep only isolated-test constructor overloads; their no-op gateway must throw if a live entity attachment is attempted.

- [ ] **Step 4: Re-run the wiring test**

Run: `gradle test --tests dev.linqfy.bigCasares.BetterModelModuleWiringTest`

Expected: `BUILD SUCCESSFUL`.

### Task 4: Render the Nexus with BetterModel

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/nexus/JavaNexusVisualGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/nexus/NexusModelAnimationPolicy.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/nexus/NexusModelAnimationPolicyTest.java`

- [ ] **Step 1: Write the failing animation policy test**

```java
@Test
void usesCriticalAtOrBelowOneQuarterHealth() {
    var policy = new NexusModelAnimationPolicy();
    assertEquals("critical", policy.steadyAnimation(0.25));
    assertEquals("idle", policy.steadyAnimation(0.26));
}

@Test
void usesDamagedAndDestroyedForTransientStates() {
    var policy = new NexusModelAnimationPolicy();
    assertEquals("damaged", policy.damageAnimation());
    assertEquals("destroyed", policy.destroyedAnimation());
}
```

- [ ] **Step 2: Run it and confirm it fails because the policy is absent**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.nexus.NexusModelAnimationPolicyTest`

Expected: test compilation failure.

- [ ] **Step 3: Replace the direct `ItemDisplay` lifecycle**

Keep the Ghast anchor, `TextDisplay`, particles, and persistent data. Replace model spawning and direct transform code with:

```java
JavaModelHandle model = models.attach(anchor, JavaModelKeys.NEXUS);
models.animate(model, "idle");
```

Store the handle in `VisualState`. On health transitions play `idle` or `critical`; on damage play `damaged`; on destruction play `destroyed`, then close the handle after its configured animation duration. `remove`, recovery cleanup, and shutdown must close the handle before removing the anchor. Delete `modelItem`, `ItemStack`, `ItemMeta`, `ItemDisplay`, and transform imports.

- [ ] **Step 4: Run Nexus regressions**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.nexus.NexusModelAnimationPolicyTest --tests dev.linqfy.bigCasares.modules.nexus.NexusVisualRecoveryTest --tests dev.linqfy.bigCasares.modules.nexus.NexusModuleWiringTest`

Expected: `BUILD SUCCESSFUL`.

### Task 5: Render the Abyss Guardian with BetterModel

**Files:**
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperAbyssGuardianRuntime.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/PaperBossAnimationGateway.java`
- Create: `src/main/java/dev/linqfy/bigCasares/modules/pveboss/BossModelAnimationMapper.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/pveboss/BossModelAnimationMapperTest.java`

- [ ] **Step 1: Write the failing configured-animation mapper test**

```java
@Test
void mapsTheSupportedBossAnimationNames() {
    var mapper = new BossModelAnimationMapper();
    assertEquals("idle", mapper.forDefinition("idle"));
    assertEquals("cast", mapper.forDefinition("cast"));
    assertEquals("rage", mapper.forDefinition("rage"));
    assertEquals("death", mapper.forDefinition("death"));
}
```

- [ ] **Step 2: Run it and confirm it fails because the mapper is absent**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.pveboss.BossModelAnimationMapperTest`

Expected: test compilation failure.

- [ ] **Step 3: Attach BetterModel to the existing Warden**

```java
JavaModelHandle model = models.attach(boss, JavaModelKeys.ABYSS_GUARDIAN);
models.animate(model, "idle");
```

Change `Instance.model` from `ItemDisplay` to `JavaModelHandle`. Remove manual `Transformation`, interpolation duration, and model teleporting from `updateInstance`. Call `models.animate(instance.model, mapper.forDefinition(ability.animationId()))` when a cast begins, `rage` on the enraged phase, and `death` before cleanup. Keep `PaperBossAnimationGateway.notifyBedrockAnimation` only for Bedrock state notifications. Close the handle before removing the Warden.

- [ ] **Step 4: Run boss regressions**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.pveboss.BossModelAnimationMapperTest --tests dev.linqfy.bigCasares.modules.pveboss.BossAbilityCastTest --tests dev.linqfy.bigCasares.modules.pveboss.PveBossModuleWiringTest`

Expected: `BUILD SUCCESSFUL`.

### Task 6: Merge BetterModel and BigCasares Java assets

**Files:**
- Create: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/MergedJavaResourcePackBuilder.java`
- Modify: `src/main/java/dev/linqfy/bigCasares/modules/resourcepack/ResourcePackBuildMain.java`
- Create: `src/test/java/dev/linqfy/bigCasares/modules/resourcepack/MergedJavaResourcePackBuilderTest.java`

- [ ] **Step 1: Write the failing merge test**

```java
@Test
void retainsAssetsFromBothPacks() throws Exception {
    Path merged = new MergedJavaResourcePackBuilder().merge(bigCasaresPack, betterModelPack, output);
    assertTrue(zipEntries(merged).contains("assets/bigcasares/models/nexus.json"));
    assertTrue(zipEntries(merged).contains("assets/bettermodel/models/bigcasares_nexus.json"));
}

@Test
void rejectsConflictingNonMetadataEntries() {
    assertThrows(IllegalStateException.class, () -> builder.merge(first, second, output));
}
```

- [ ] **Step 2: Run it and confirm it fails because the builder is absent**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.resourcepack.MergedJavaResourcePackBuilderTest`

Expected: test compilation failure.

- [ ] **Step 3: Implement deterministic merging**

Merge the generated BigCasares ZIP and BetterModel ZIP. Preserve BigCasares `pack.mcmeta` and pack icon, copy every non-conflicting BetterModel entry, and throw on a duplicate non-metadata path. Add `--bettermodel-java-pack=<zip>` to `ResourcePackBuildMain`; without it, preserve the existing BigCasares-only result. Document BetterModel settings: `merge-with-external-resources: true`, a non-`bigcasares` namespace, and `pack-type: folder` while inspecting model output.

- [ ] **Step 4: Run resource-pack regressions**

Run: `gradle test --tests dev.linqfy.bigCasares.modules.resourcepack.MergedJavaResourcePackBuilderTest --tests dev.linqfy.bigCasares.modules.resourcepack.ResourcePackBuilderTest --tests dev.linqfy.bigCasares.modules.resourcepack.ResourcePackValidatorTest`

Expected: `BUILD SUCCESSFUL`.

### Task 7: Write the Blockbench and BetterModel handbook

**Files:**
- Modify: `docs/blockbench/01-boss-models-overview.md`
- Modify: `docs/blockbench/04-java-animations-and-registration.md`
- Create: `docs/blockbench/00-bettermodel-setup.md`
- Create: `docs/blockbench/05-placeable-entity-workflow.md`
- Create: `docs/blockbench/06-boss-entity-workflow.md`
- Create: `docs/blockbench/07-player-animation-workflow.md`

- [ ] **Step 1: Document setup and pack deployment**

Include these exact authoring paths and commands:

```text
plugins/BetterModel/models/bigcasares_nexus.bbmodel
plugins/BetterModel/models/bigcasares_abyss_guardian.bbmodel
/bettermodel reload
/bettermodel spawn bigcasares_nexus ghast 1
```

- [ ] **Step 2: Update the Java overview and animation guide**

Replace the obsolete flat-model and server-side transform workflow with BetterModel's tracker architecture: one display cost per Blockbench bone, named animations, entity-attached trackers, and a distinct Bedrock export (`.geo.json`, animation JSON, controller JSON).

- [ ] **Step 3: Add placeable entity documentation**

Describe the Nexus lifecycle exactly:

```text
custom item placement -> validate team and location -> invisible anchor
-> BetterModel attachment -> named state animation -> persist gameplay state
-> close model before removing anchor
```

Cover root bone rules, offsets, `idle`/`damaged`/`critical`/`destroyed`, recovery after restart, and manual tests for placement, attack, low health, destruction, and team ownership.

- [ ] **Step 4: Add boss and player-animation documentation**

Explain the invisible Warden + tracker + ability-animation pattern for bosses. Explain BetterModel player assets under `plugins/BetterModel/players/`, `BetterModel.limb(...)`, player profile ownership, disconnect/death restoration, and the rule that animation never owns player combat, inventory, or movement. Include performance advice: use meaningful bones only because every bone costs display packets.

- [ ] **Step 5: Check for stale guidance**

Run: `Get-ChildItem docs/blockbench -Filter *.md | Select-String -Pattern 'ItemDisplay|ArmorStand'`

Expected: legacy terms occur only in explicit migration warnings, not as the recommended Java entity-model workflow.

### Task 8: Full verification and commit

**Files:**
- Review all files changed by Tasks 1-7.

- [ ] **Step 1: Run the full suite and package build**

Run: `gradle test; gradle clean build generateResourcePacks`

Expected: both commands report `BUILD SUCCESSFUL`; the normal Java ZIP and Bedrock MCPACK exist.

- [ ] **Step 2: Verify merged pack behavior**

Run after placing BetterModel's generated ZIP at `build/bettermodel/bettermodel-java.zip`:

```powershell
gradle generateResourcePacks --args "resourcepack build/generated-resourcepacks --bettermodel-java-pack=build/bettermodel/bettermodel-java.zip"
```

Expected: one Java ZIP contains both `assets/bigcasares/` and `assets/bettermodel/`.

- [ ] **Step 3: Run the Paper smoke test**

Install BetterModel 3.2.0 into `build/run-server/plugins/`, then run `gradle runServer`.

Expected: BetterModel enables before BigCasares; BigCasares reaches `Done` on port 25565. In game, verify Nexus `idle`, `damaged`, `critical`, `destroyed`; Abyss Guardian `idle`, `cast`, `rage`, `death`; and a Geyser client still gets native Bedrock visuals.

- [ ] **Step 4: Review and commit**

Run `git diff --check` and `git status --short`, then commit only the planned implementation, test, resource-pack, and documentation changes:

```text
feat(models): render Nexus and bosses with BetterModel
```
