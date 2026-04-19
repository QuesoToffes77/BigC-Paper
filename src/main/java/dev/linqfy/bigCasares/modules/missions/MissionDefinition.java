package dev.linqfy.bigCasares.modules.missions;

import java.util.Map;

public record MissionDefinition(
    String id,
    MissionScope scope,
    String title,
    String description,
    MissionType type,
    int goal,
    double reward,
    int weight,
    boolean enabled,
    Map<String, Object> params
) {
}
