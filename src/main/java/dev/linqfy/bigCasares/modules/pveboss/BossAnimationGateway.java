package dev.linqfy.bigCasares.modules.pveboss;

import java.util.UUID;

public interface BossAnimationGateway {
    void applyFrame(UUID bossId, BossAnimationFrame frame, int interpolationDurationTicks);
    void notifyBedrockAnimation(UUID bossId, String animationId);
}
