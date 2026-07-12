package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public final class BossModelAnimationMapper {

    public String forDefinition(String animationId) {
        Objects.requireNonNull(animationId, "animationId");
        String key = animationId.strip();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Boss animation id must not be blank");
        }
        return key;
    }
}
