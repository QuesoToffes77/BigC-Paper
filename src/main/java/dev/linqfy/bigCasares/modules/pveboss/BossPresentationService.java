package dev.linqfy.bigCasares.modules.pveboss;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BossPresentationService {

    private final BossPresentationGateway gateway;
    private final Map<UUID, VisibleBoss> visibleBosses = new HashMap<>();

    public BossPresentationService(BossPresentationGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public boolean showBoss(BossAudience audience, BossView view) {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(view, "view");
        VisibleBoss current = visibleBosses.get(view.bossInstanceId());
        if (current != null && current.audience.equals(audience) && current.view.equals(view)) {
            return false;
        }
        if (current != null) {
            gateway.hideBoss(current.audience, view.bossInstanceId());
        }
        visibleBosses.put(view.bossInstanceId(), new VisibleBoss(audience, view));
        gateway.showBoss(audience, view);
        return true;
    }

    public boolean updateHealth(BossHealthView health) {
        Objects.requireNonNull(health, "health");
        VisibleBoss visible = visibleBosses.get(health.bossInstanceId());
        if (visible == null || visible.view.health().equals(health)) {
            return false;
        }
        visible.view = visible.view.withHealth(health);
        gateway.updateHealth(visible.audience, health);
        return true;
    }

    public boolean updatePhase(BossPhaseView phase) {
        Objects.requireNonNull(phase, "phase");
        VisibleBoss visible = visibleBosses.get(phase.bossInstanceId());
        if (visible == null || visible.view.phase().equals(phase)) {
            return false;
        }
        visible.view = visible.view.withPhase(phase);
        gateway.updatePhase(visible.audience, phase);
        return true;
    }

    public boolean showAbilityCast(BossAbilityCastView ability) {
        Objects.requireNonNull(ability, "ability");
        VisibleBoss visible = visibleBosses.get(ability.bossInstanceId());
        if (visible == null || ability.equals(visible.lastAbilityCast)) {
            return false;
        }
        visible.lastAbilityCast = ability;
        gateway.showAbilityCast(visible.audience, ability);
        return true;
    }

    public boolean showAnnouncement(BossAnnouncement announcement) {
        Objects.requireNonNull(announcement, "announcement");
        VisibleBoss visible = visibleBosses.get(announcement.bossInstanceId());
        if (visible == null) {
            return false;
        }
        gateway.showAnnouncement(visible.audience, announcement);
        return true;
    }

    public boolean defeatBoss(UUID bossInstanceId) {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        VisibleBoss removed = visibleBosses.remove(bossInstanceId);
        if (removed == null) {
            return false;
        }
        gateway.hideBoss(removed.audience, bossInstanceId);
        return true;
    }

    public boolean isVisible(UUID bossInstanceId) {
        return visibleBosses.containsKey(Objects.requireNonNull(bossInstanceId, "bossInstanceId"));
    }

    private static final class VisibleBoss {
        private final BossAudience audience;
        private BossView view;
        private BossAbilityCastView lastAbilityCast;

        private VisibleBoss(BossAudience audience, BossView view) {
            this.audience = audience;
            this.view = view;
        }
    }
}
