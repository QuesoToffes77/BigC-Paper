package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropSettingsTest {

    @Test
    void automaticDefaultsToTrueWhenMissing() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.interval-minutes", 30);

        AirdropSettings settings = AirdropSettings.fromConfig(config);

        assertTrue(settings.automatic());
    }

    @Test
    void automaticCanBeDisabled() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.automatic", false);
        config.set("airdrop-system.interval-minutes", 30);

        AirdropSettings settings = AirdropSettings.fromConfig(config);

        assertFalse(settings.automatic());
    }

    @Test
    void intervalIsReadFromConfig() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.interval-minutes", 45);

        AirdropSettings settings = AirdropSettings.fromConfig(config);

        assertEquals(45, settings.intervalMinutes());
    }

    @Test
    void missingKeysFallBackToDefaults() {
        AirdropSettings settings = AirdropSettings.fromConfig(new YamlConfiguration());

        assertTrue(settings.automatic());
        assertEquals(30, settings.intervalMinutes());
        assertEquals(10000, settings.radius());
        assertEquals(30, settings.dropHeight());
    }

    @Test
    void nonPositiveIntervalIsRejected() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.interval-minutes", 0);

        assertThrows(IllegalArgumentException.class, () -> AirdropSettings.fromConfig(config));
    }

    @Test
    void mobSettingsHaveBoundedDefaults() {
        AirdropSettings settings = AirdropSettings.fromConfig(new YamlConfiguration());
        AirdropMobSettings mobs = settings.mobs();

        assertTrue(mobs.enabled());
        assertEquals(40, mobs.maxTotal());
        assertEquals(20, mobs.zombies().amount());
        assertEquals(12, mobs.zombies().radius());
        assertEquals(40, mobs.zombies().gearChance());
        assertTrue(mobs.horseRiders().enabled());
        assertEquals(2, mobs.horseRiders().amount());
        assertEquals(20, mobs.horseRiders().chancePercent());
        assertTrue(mobs.phantomCreepers().enabled());
        assertEquals(1, mobs.phantomCreepers().amountFor(AirdropQuality.EPIC));
        assertEquals(2, mobs.phantomCreepers().amountFor(AirdropQuality.LEGENDARY));
        assertEquals(3, mobs.phantomCreepers().amountFor(AirdropQuality.GHISTIC));
        assertEquals(36_000, mobs.phantomCreepers().fireResistanceTicks());
    }

    @Test
    void loadsCustomMobSettings() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.mobs.enabled", false);
        config.set("airdrop-system.mobs.max-total", 30);
        config.set("airdrop-system.mobs.zombies.amount", 15);
        config.set("airdrop-system.mobs.zombies.radius", 8);
        config.set("airdrop-system.mobs.zombies.gear-chance", 60);
        config.set("airdrop-system.mobs.horse-riders.enabled", false);
        config.set("airdrop-system.mobs.horse-riders.amount", 1);
        config.set("airdrop-system.mobs.horse-riders.chance", 10);
        config.set("airdrop-system.mobs.phantom-creepers.enabled", true);
        config.set("airdrop-system.mobs.phantom-creepers.epic-amount", 2);
        config.set("airdrop-system.mobs.phantom-creepers.legendary-amount", 3);
        config.set("airdrop-system.mobs.phantom-creepers.ghistic-amount", 4);
        config.set("airdrop-system.mobs.phantom-creepers.fire-resistance-minutes", 45);
        config.set("airdrop-system.mobs.phantom-creepers.spawn-height", 18);

        AirdropMobSettings mobs = AirdropSettings.fromConfig(config).mobs();

        assertFalse(mobs.enabled());
        assertEquals(30, mobs.maxTotal());
        assertEquals(15, mobs.zombies().amount());
        assertEquals(8, mobs.zombies().radius());
        assertEquals(60, mobs.zombies().gearChance());
        assertFalse(mobs.horseRiders().enabled());
        assertEquals(1, mobs.horseRiders().amount());
        assertEquals(10, mobs.horseRiders().chancePercent());
        assertEquals(2, mobs.phantomCreepers().amountFor(AirdropQuality.EPIC));
        assertEquals(3, mobs.phantomCreepers().amountFor(AirdropQuality.LEGENDARY));
        assertEquals(4, mobs.phantomCreepers().amountFor(AirdropQuality.GHISTIC));
        assertEquals(54_000, mobs.phantomCreepers().fireResistanceTicks());
        assertEquals(18, mobs.phantomCreepers().spawnHeight());
    }

    @Test
    void absurdMobValuesAreClamped() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.mobs.max-total", 999);
        config.set("airdrop-system.mobs.zombies.amount", 999);
        config.set("airdrop-system.mobs.zombies.radius", 999);
        config.set("airdrop-system.mobs.zombies.gear-chance", 999);
        config.set("airdrop-system.mobs.horse-riders.amount", 999);
        config.set("airdrop-system.mobs.horse-riders.chance", 999);

        AirdropMobSettings mobs = AirdropSettings.fromConfig(config).mobs();

        assertEquals(100, mobs.maxTotal());
        assertEquals(64, mobs.zombies().amount());
        assertEquals(64, mobs.zombies().radius());
        assertEquals(100, mobs.zombies().gearChance());
        assertEquals(16, mobs.horseRiders().amount());
        assertEquals(100, mobs.horseRiders().chancePercent());
    }

    @Test
    void disabledMobSettingsPlanNoDefenders() {
        AirdropSettings settings = AirdropSettings.fromConfig(new YamlConfiguration());
        AirdropMobSettings disabled = AirdropMobSettings.disabled();

        assertFalse(disabled.enabled());
        assertEquals(0, disabled.maxTotal());
        assertEquals(new AirdropDefenderMobs.Plan(0, 0),
            AirdropDefenderMobs.plan(disabled, new java.util.Random(1)));
        assertTrue(settings.mobs().enabled());
    }

    @Test
    void qualityConfigurationLoadsAllFiveQualities() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("airdrop-system.quality-chances.COMMON", 0.39);
        config.set("airdrop-system.quality-chances.RARE", 0.30);
        config.set("airdrop-system.quality-chances.EPIC", 0.20);
        config.set("airdrop-system.quality-chances.LEGENDARY", 0.10);
        config.set("airdrop-system.quality-chances.GHISTIC", 0.01);

        AirdropQualitySettings quality = AirdropSettings.fromConfig(config).quality();

        assertEquals(1.0, quality.totalChance(), 0.000_001);
        assertEquals(0.01, quality.chances().get(AirdropQuality.GHISTIC));
        assertEquals(15, quality.profile(AirdropQuality.GHISTIC).guardCount());
    }

    @Test
    void invalidChanceSumFallsBackToSafeDefaults() {
        YamlConfiguration config = new YamlConfiguration();
        for (AirdropQuality quality : AirdropQuality.values()) {
            config.set("airdrop-system.quality-chances." + quality.name(), 1.0);
        }

        AirdropQualitySettings result = AirdropSettings.fromConfig(config).quality();

        assertEquals(AirdropQualitySettings.defaults().chances(), result.chances());
    }
}
