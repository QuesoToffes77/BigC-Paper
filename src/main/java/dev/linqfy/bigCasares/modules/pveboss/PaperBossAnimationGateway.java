package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.modules.geyser.GeyserBossVisualGateway;

import java.util.UUID;

public class PaperBossAnimationGateway implements BossAnimationGateway {

    private final GeyserBossVisualGateway geyserGateway;

    public PaperBossAnimationGateway(GeyserBossVisualGateway geyserGateway) {
        this.geyserGateway = geyserGateway;
    }

    @Override
    public void applyFrame(UUID bossId, BossAnimationFrame frame, int interpolationDurationTicks) {
        // BetterModel plays named Blockbench animations; Java does not consume legacy transform frames.
    }

    @Override
    public void notifyBedrockAnimation(UUID bossId, String animationId) {
        if (geyserGateway != null) {
            geyserGateway.updatePhase(bossId, animationId);
        }
    }
}
