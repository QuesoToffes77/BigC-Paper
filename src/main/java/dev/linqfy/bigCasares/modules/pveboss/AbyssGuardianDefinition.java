package dev.linqfy.bigCasares.modules.pveboss;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AbyssGuardianDefinition(
    String id,
    String displayName,
    double maximumHealth,
    double audienceRadius,
    int updateTicks,
    double phaseTwoAt,
    double phaseThreeAt,
    List<BossAbilityDefinition> abilities,
    Map<String, List<String>> dialogue,
    Map<Integer, BossMusicTrack> musicByPhase,
    Map<String, BossAnimationDefinition> animations,
    Map<BossBehaviourCategory, Double> categoryWeights
) {
    public AbyssGuardianDefinition {
        id = requireText(id, "id");
        displayName = requireText(displayName, "displayName");
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0) {
            throw new IllegalArgumentException("maximumHealth must be positive");
        }
        if (!Double.isFinite(audienceRadius) || audienceRadius <= 0.0) {
            throw new IllegalArgumentException("audienceRadius must be positive");
        }
        if (updateTicks < 1) {
            throw new IllegalArgumentException("updateTicks must be positive");
        }
        if (phaseTwoAt <= phaseThreeAt || phaseTwoAt >= 1.0 || phaseThreeAt <= 0.0) {
            throw new IllegalArgumentException("phase thresholds must descend inside (0, 1)");
        }
        abilities = List.copyOf(abilities);
        if (abilities.isEmpty()) {
            throw new IllegalArgumentException("at least one ability is required");
        }
        dialogue = Map.copyOf(dialogue);
        musicByPhase = Map.copyOf(musicByPhase);
        animations = animations == null ? Map.of() : Map.copyOf(animations);
        categoryWeights = categoryWeights == null ? Map.of() : Map.copyOf(categoryWeights);
    }

    public BossAnimationDefinition animationFor(String id) {
        return animations.get(id);
    }

    public int phaseFor(double healthFraction) {
        if (healthFraction <= phaseThreeAt) {
            return 3;
        }
        return healthFraction <= phaseTwoAt ? 2 : 1;
    }

    public BossMusicTrack musicForPhase(int phase) {
        return Objects.requireNonNull(musicByPhase.get(phase), "music for phase " + phase);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }
}
