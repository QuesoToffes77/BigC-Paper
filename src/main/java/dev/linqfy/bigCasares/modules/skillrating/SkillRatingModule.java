package dev.linqfy.bigCasares.modules.skillrating;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class SkillRatingModule implements PluginModule {

    private static final double DEFAULT_MU = 25.0;
    private static final double DEFAULT_SIGMA = DEFAULT_MU / 3.0;
    private static final double DEFAULT_ORDINAL_SIGMA_MULTIPLIER = 3.0;
    private static final double DEFAULT_SKILL_RATING_SCALE = 100.0;

    private final BigCasares plugin;

    private SkillRatingService service;
    private boolean enabled;

    public SkillRatingModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "skill-rating-system";
    }

    @Override
    public void onEnable() {
        SkillRatingSettings settings = new SkillRatingSettings(
            plugin.getConfig().getDouble("skill-rating-system.default-mu", DEFAULT_MU),
            plugin.getConfig().getDouble("skill-rating-system.default-sigma", DEFAULT_SIGMA),
            plugin.getConfig().getDouble("skill-rating-system.ordinal-sigma-multiplier", DEFAULT_ORDINAL_SIGMA_MULTIPLIER),
            plugin.getConfig().getDouble("skill-rating-system.skill-rating-scale", DEFAULT_SKILL_RATING_SCALE),
            tierThresholds()
        );
        Path playersDirectory = plugin.getDataFolder().toPath().resolve("data").resolve("skill-rating").resolve("players");
        SkillRatingStorage storage = new YamlSkillRatingStorage(playersDirectory);
        this.service = new SkillRatingService(storage, settings, Instant::now);
        plugin.getServer().getPluginManager().registerEvents(new SkillRatingListener(service), plugin);
        this.enabled = true;
    }

    @Override
    public void onDisable() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SkillRatingService service() {
        return service;
    }

    private double[] tierThresholds() {
        List<Double> configured = plugin.getConfig().getDoubleList("skill-rating-system.tier-thresholds");
        if (configured.size() != 4) {
            return new double[] {500.0, 1500.0, 2500.0, 3500.0};
        }
        return configured.stream()
            .mapToDouble(Double::doubleValue)
            .toArray();
    }
}
