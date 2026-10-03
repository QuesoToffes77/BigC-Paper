# Blood Moon and Environmental Visuals Implementation Plan

## Objective

Deliver Blood Moon and four distinct environmental visual profiles on Paper 26.2 / Java 25 without changing unrelated modules or replacing global vanilla weather/sky resources.

## Main Files

- `src/main/java/dev/linqfy/bigCasares/modules/bloodmoon/`
- `src/main/java/dev/linqfy/bigCasares/modules/environment/`
- `src/main/java/dev/linqfy/bigCasares/modules/acidrain/AcidRainRuntime.java`
- `src/main/java/dev/linqfy/bigCasares/modules/acidrain/AcidRainSettings.java`
- `src/main/java/dev/linqfy/bigCasares/modules/acidrain/AcidRainSettingsLoader.java`
- `src/main/java/dev/linqfy/bigCasares/BigCasares.java`
- `src/main/resources/config.yml`
- `src/main/resources/plugin.yml`

## Tasks

1. Inspect module, runtime scope, command, config, Acid Rain, resource-pack, and test conventions.
2. Write RED tests for scheduling, modifiers, cleanup, spawning, settings, and visual profiles.
3. Implement pure domain policies and make targeted tests GREEN.
4. Implement Bukkit attribute, spawn, feedback, command, listener, and lifecycle adapters.
5. Integrate the module and Acid Rain visual profiles.
6. Add command/permission/default configuration contracts.
7. Verify normal vanilla rain/moon/fog resources are not replaced.
8. Run the full test suite.
9. Run `gradlew.bat clean build` without skipping tests.
10. Deploy the JAR to the local Paper test server and perform a startup/command smoke test.

## Risks and Controls

- Attribute accumulation: stable keyed modifiers and duplicate checks.
- Other-plugin stat loss: never changes attribute base values and removes only owned keys.
- Spawn pressure: loaded chunks, gamerules, block checks, vanilla-derived ceiling, and two event caps.
- Passive/player buffs: eligibility rejects players and defaults to `Enemy`.
- Reload leftovers: runtime scope cleanup plus stable-key reconciliation on entity load.
- Visual overload: per-player profiles, clamped radius/cadence, and hard particle budgets.
- Global texture regressions: no rain, moon, fog, biome, or shader resource replacement.

## Success Criteria

- Blood Moon starts only at night and ends at sunrise.
- Chance and interval schedules work once per night.
- Hostile max health is `1.5x` and movement speed is `1.25x` by default.
- Modifiers never stack and cleanup preserves external modifiers.
- Extra spawning stays inside configured distance and pressure limits.
- Acid Rain, Toxic Spores, Chemical Fog, and Blood Moon are visually distinct.
- Normal nights, rain, fog, mobs, and players remain unchanged outside events.
- Full tests and clean build pass.

## Agent Completion Report

### Created

- Blood Moon implementation under `src/main/java/dev/linqfy/bigCasares/modules/bloodmoon/`.
- Shared visual profiles under `src/main/java/dev/linqfy/bigCasares/modules/environment/`.
- Blood Moon and environmental visual tests under matching `src/test/java` packages.
- This plan and the matching design specification.

### Modified

- `BigCasares.java`: module construction, registration, getter, and Acid Rain conflict supplier.
- `AcidRainModule.java`: minimal public running-state query.
- `AcidRainRuntime.java`, `AcidRainSettings.java`, and `AcidRainSettingsLoader.java`: visual profile integration only.
- `config.yml`: Blood Moon and visual-quality defaults.
- `plugin.yml`: `/bloodmoon` and admin/start/stop permissions.

### Dependencies and Assets

- No dependency or version changes.
- No PNG/model/shader assets added because the vanilla client cannot select them dynamically per event.
- Normal rain, moon, fog, biome, and shader resources remain untouched.

### Verification

- Tests: 848 executed, 848 passed, 0 failed.
- Build: `gradlew.bat clean build` successful without skipped tests.
- Java resource pack: `build/generated-resourcepacks/bigcasares-java.zip` regenerated.
- JAR: `build/libs/BigCasares-0.0.1.jar`.
- Paper smoke test: 28/28 modules enabled; Blood Moon automatic start, duplicate rejection, status, sunrise stop, and clean disable verified.
- Real entity attributes with Mob Scaling present: max health `50.912688 -> 76.369022` (`1.5x`) and speed `0.483 -> 0.60375` (`1.25x`).
- Minecraft graphical client rendering: not tested.
