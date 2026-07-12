package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AbyssGuardianDefinitionLoader {

    public AbyssGuardianDefinition load(ConfigurationSection root) {
        ConfigurationSection abilities = requireSection(root, "abilities");
        List<BossAbilityDefinition> parsedAbilities = abilities.getKeys(false).stream()
            .map(id -> loadAbility(id, requireSection(abilities, id)))
            .toList();

        LinkedHashMap<String, List<String>> dialogue = new LinkedHashMap<>();
        ConfigurationSection dialogueSection = root.getConfigurationSection("dialogue");
        if (dialogueSection != null) {
            dialogueSection.getKeys(false).forEach(key -> dialogue.put(key, dialogueSection.getStringList(key)));
        }

        String fallback = root.getString("music.fallback-sound", "minecraft:music_disc.5");
        LinkedHashMap<Integer, BossMusicTrack> tracks = new LinkedHashMap<>();
        for (int phase = 1; phase <= 3; phase++) {
            String id = "phase-" + phase;
            tracks.put(phase, new BossMusicTrack(
                id,
                root.getString("music.tracks." + id, "bigcasares:music.abyss_guardian"),
                fallback
            ));
        }

        return new AbyssGuardianDefinition(
            root.getString("id", "abyss-guardian"),
            root.getString("display-name", "GUARDIÁN DEL ABISMO"),
            root.getDouble("maximum-health", 20_000.0),
            root.getDouble("audience-radius", 64.0),
            root.getInt("ui.update-ticks", 5),
            root.getDouble("phases.phase-2-at", 0.66),
            root.getDouble("phases.phase-3-at", 0.33),
            parsedAbilities,
            dialogue,
            tracks,
            loadAnimations(root.getConfigurationSection("animations")),
            loadCategoryWeights(root.getConfigurationSection("category-weights"))
        );
    }

    private BossAbilityDefinition loadAbility(String id, ConfigurationSection section) {
        ConfigurationSection target = requireSection(section, "targets");
        BossTargetSelectorType targetType = enumValue(
            BossTargetSelectorType.class, target.getString("type", "NEAREST_PLAYER"));
        double radius = target.getDouble("radius", Double.POSITIVE_INFINITY);
        int maxTargets = targetType == BossTargetSelectorType.ALL_IN_RADIUS
            ? Integer.MAX_VALUE : target.getInt("max-targets", 1);

        String categoryStr = section.getString("category", "PHYSICAL");
        BossBehaviourCategory category = BossBehaviourCategory.valueOf(categoryStr.toUpperCase());
        String animationId = section.getString("animation", "cast");

        return new BossAbilityDefinition(
            id,
            seconds(section.getDouble("cooldown-seconds", 18.0)),
            seconds(section.getDouble("cast-time-seconds", 3.0)),
            new BossTargetSelectorDefinition(targetType, radius, maxTargets),
            loadConditions(section.getMapList("conditions")),
            loadEffects(section.getMapList("effects")),
            loadTelegraph(section.getConfigurationSection("telegraph")),
            section.getBoolean("interruptible", false),
            section.getInt("priority", 0),
            animationId,
            category,
            section.getDouble("combo-chance", 0.0),
            section.getString("next-combo-ability", "")
        );
    }

    private List<BossAbilityCondition> loadConditions(List<Map<?, ?>> raw) {
        return raw.stream().map(this::loadCondition).toList();
    }

    private BossAbilityCondition loadCondition(Map<?, ?> raw) {
        BossAbilityConditionType type = enumValue(
            BossAbilityConditionType.class, String.valueOf(raw.get("type")));
        if (type == BossAbilityConditionType.NOT_CASTING || type == BossAbilityConditionType.COOLDOWN_READY) {
            return BossAbilityCondition.noValue(type);
        }
        Object value = raw.get("value");
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("Boss condition " + type + " requires a numeric value");
        }
        return new BossAbilityCondition(type, number.doubleValue());
    }

    private List<BossAbilityEffectDefinition> loadEffects(List<Map<?, ?>> raw) {
        return raw.stream().map(this::loadEffect).toList();
    }

    private BossAbilityEffectDefinition loadEffect(Map<?, ?> raw) {
        BossAbilityEffectType type = enumValue(
            BossAbilityEffectType.class, String.valueOf(raw.get("type")));
        LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (!"type".equals(String.valueOf(key))) {
                parameters.put(String.valueOf(key), value);
            }
        });
        return new BossAbilityEffectDefinition(type, parameters);
    }

    private BossTelegraphDefinition loadTelegraph(ConfigurationSection telegraph) {
        if (telegraph == null) return null;
        return new BossTelegraphDefinition(
            enumValue(BossTelegraphType.class, telegraph.getString("type", "CIRCLE")),
            telegraph.getDouble("radius", 5.0),
            telegraph.getString("particle", "minecraft:dust"),
            telegraph.getString("warning-sound", "minecraft:block.beacon.power_select")
        );
    }

    private LinkedHashMap<String, List<String>> loadDialogue(ConfigurationSection section) {
        LinkedHashMap<String, List<String>> dialogue = new LinkedHashMap<>();
        if (section != null) {
            section.getKeys(false).forEach(key -> dialogue.put(key, section.getStringList(key)));
        }
        return dialogue;
    }

    private LinkedHashMap<Integer, BossMusicTrack> loadMusic(ConfigurationSection section) {
        String fallback = section != null ? section.getString("fallback-sound", "minecraft:music_disc.5") : "minecraft:music_disc.5";
        LinkedHashMap<Integer, BossMusicTrack> tracks = new LinkedHashMap<>();
        for (int phase = 1; phase <= 3; phase++) {
            String id = "phase-" + phase;
            tracks.put(phase, new BossMusicTrack(
                id,
                section != null ? section.getString("tracks." + id, "bigcasares:music.abyss_guardian") : "bigcasares:music.abyss_guardian",
                fallback
            ));
        }
        return tracks;
    }

    private static Duration seconds(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("duration must be finite and non-negative");
        }
        return Duration.ofMillis(Math.round(value * 1000.0));
    }

    private static ConfigurationSection requireSection(ConfigurationSection parent, String path) {
        ConfigurationSection section = parent.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalArgumentException("Missing boss configuration section: " + path);
        }
        return section;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
    }

    private Map<String, BossAnimationDefinition> loadAnimations(ConfigurationSection section) {
        if (section == null) return Map.of();
        Map<String, BossAnimationDefinition> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection anim = section.getConfigurationSection(key);
            if (anim == null) continue;

            BossAnimationType type = BossAnimationType.valueOf(anim.getString("type", "ONE_SHOT").toUpperCase());
            int durationTicks = anim.getInt("duration-ticks", 20);

            List<Double> scaleList = anim.getDoubleList("scale");
            float startScale = scaleList.size() > 0 ? scaleList.get(0).floatValue() : 1.0f;
            float endScale = scaleList.size() > 1 ? scaleList.get(1).floatValue() : startScale;

            List<Double> yOffsetList = anim.getDoubleList("y-offset");
            float startYOffset = yOffsetList.size() > 0 ? yOffsetList.get(0).floatValue() : 0.0f;
            float endYOffset = yOffsetList.size() > 1 ? yOffsetList.get(1).floatValue() : startYOffset;

            List<Double> rotList = anim.getDoubleList("y-rotation");
            float startYRot = rotList.size() > 0 ? rotList.get(0).floatValue() : 0.0f;
            float endYRot = rotList.size() > 1 ? rotList.get(1).floatValue() : startYRot;

            int priority = anim.getInt("priority", 0);

            map.put(key, new BossAnimationDefinition(key, type, durationTicks, startScale, endScale, startYOffset, endYOffset, startYRot, endYRot, priority));
        }
        return map;
    }

    private Map<BossBehaviourCategory, Double> loadCategoryWeights(ConfigurationSection section) {
        if (section == null) return Map.of();
        Map<BossBehaviourCategory, Double> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                BossBehaviourCategory category = BossBehaviourCategory.valueOf(key.toUpperCase());
                map.put(category, section.getDouble(key));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return map;
    }
}
