package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;

public record NexusVisualRequest(
        NexusId nexusId,
        NexusPosition position,
        String teamName,
        double currentHealth,
        double maximumHealth,
        String itemModelId
) {

    public NexusVisualRequest {
        Objects.requireNonNull(nexusId, "nexusId");
        Objects.requireNonNull(position, "position");
        teamName = requireText(teamName, "teamName");
        itemModelId = requireText(itemModelId, "itemModelId");
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0) {
            throw new IllegalArgumentException("maximumHealth must be finite and positive");
        }
        if (!Double.isFinite(currentHealth) || currentHealth < 0.0 || currentHealth > maximumHealth) {
            throw new IllegalArgumentException("currentHealth must be between zero and maximumHealth");
        }
    }

    public double healthFraction() {
        return currentHealth / maximumHealth;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String stripped = value.strip();
        if (stripped.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return stripped;
    }
}
