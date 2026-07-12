package dev.linqfy.bigCasares.modules.pveboss;

import java.util.UUID;

public interface BossPresentationGateway {

    void showBoss(BossAudience audience, BossView view);

    void updateHealth(BossAudience audience, BossHealthView health);

    void updatePhase(BossAudience audience, BossPhaseView phase);

    void showAbilityCast(BossAudience audience, BossAbilityCastView ability);

    void showAnnouncement(BossAudience audience, BossAnnouncement announcement);

    void hideBoss(BossAudience audience, UUID bossInstanceId);
}
