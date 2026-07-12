package dev.linqfy.bigCasares.modules.pveboss;

public record BossAnimationDefinition(
    String id,
    BossAnimationType type,
    int durationTicks,
    float startScale,
    float endScale,
    float startYOffset,
    float endYOffset,
    float startYRotation,
    float endYRotation,
    int priority
) {}
