package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Objects;
import java.util.List;
import java.util.UUID;

public record AirdropData(
        UUID id,
        AirdropPosition position,
        AirdropType type,
        AirdropQuality quality,
        AirdropPhase phase,
        boolean lootGenerated,
        List<AirdropReward> rewards
) {
    public AirdropData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(quality, "quality");
        Objects.requireNonNull(phase, "phase");
        rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
        if (!lootGenerated && !rewards.isEmpty()) {
            throw new IllegalArgumentException("unresolved loot cannot contain rewards");
        }
    }

    public AirdropData(AirdropPosition position, AirdropType type, AirdropPhase phase) {
        this(UUID.randomUUID(), position, type, AirdropQuality.COMMON, phase, false, List.of());
    }

    public boolean isActive() {
        return phase != AirdropPhase.CLAIMED;
    }

    public AirdropData withPhase(AirdropPhase phase) {
        return new AirdropData(id, position, type, quality, phase, lootGenerated, rewards);
    }

    public AirdropData withPositionAndPhase(AirdropPosition position, AirdropPhase phase) {
        return new AirdropData(id, position, type, quality, phase, lootGenerated, rewards);
    }

    public AirdropData withGeneratedLoot(List<AirdropReward> rewards) {
        return new AirdropData(id, position, type, quality, phase, true, rewards);
    }
}
