package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossPresentationServiceTest {

    @Test
    void publishesPhaseTransitionsOnlyWhileTheBossIsVisible() {
        RecordingPresentationGateway gateway = new RecordingPresentationGateway();
        BossPresentationService service = new BossPresentationService(gateway);
        BossView boss = bossView();
        BossAudience audience = new BossAudience(Set.of(UUID.randomUUID()));
        service.showBoss(audience, boss);

        boolean updated = service.updatePhase(
            new BossPhaseView(boss.bossInstanceId(), 2, "Fase II", "ENRAGED")
        );
        service.defeatBoss(boss.bossInstanceId());
        boolean updatedAfterDefeat = service.updatePhase(
            new BossPhaseView(boss.bossInstanceId(), 3, "Fase III", "FINAL")
        );

        assertTrue(updated);
        assertFalse(updatedAfterDefeat);
        assertTrue(gateway.events.contains("phase:2"));
        assertFalse(gateway.events.contains("phase:3"));
    }

    @Test
    void clearsEveryUiLayerWhenTheBossDies() {
        RecordingPresentationGateway gateway = new RecordingPresentationGateway();
        BossPresentationService service = new BossPresentationService(gateway);
        BossView boss = bossView();
        BossAudience audience = new BossAudience(Set.of(UUID.randomUUID(), UUID.randomUUID()));
        service.showBoss(audience, boss);
        service.showAbilityCast(new BossAbilityCastView(
            boss.bossInstanceId(),
            "void-pulse",
            "Pulso del Vacío",
            Duration.ofSeconds(3),
            BossTelegraphDefinition.of(BossTelegraphType.CIRCLE)
        ));

        boolean removed = service.defeatBoss(boss.bossInstanceId());

        assertTrue(removed);
        assertFalse(service.isVisible(boss.bossInstanceId()));
        assertTrue(gateway.events.contains("hide:" + boss.bossInstanceId()));
    }

    private static BossView bossView() {
        UUID bossId = UUID.randomUUID();
        return new BossView(
            bossId,
            "GUARDIÁN DEL ABISMO",
            new BossHealthView(bossId, 20_000.0, 20_000.0),
            new BossPhaseView(bossId, 1, "Fase I", ""),
            ""
        );
    }

    private static final class RecordingPresentationGateway implements BossPresentationGateway {
        private final List<String> events = new ArrayList<>();

        @Override
        public void showBoss(BossAudience audience, BossView view) {
            events.add("show:" + view.bossInstanceId());
        }

        @Override
        public void updateHealth(BossAudience audience, BossHealthView health) {
            events.add("health:" + health.currentHealth());
        }

        @Override
        public void updatePhase(BossAudience audience, BossPhaseView phase) {
            events.add("phase:" + phase.phase());
        }

        @Override
        public void showAbilityCast(BossAudience audience, BossAbilityCastView ability) {
            events.add("cast:" + ability.abilityId());
        }

        @Override
        public void showAnnouncement(BossAudience audience, BossAnnouncement announcement) {
            events.add("announcement:" + announcement.message());
        }

        @Override
        public void hideBoss(BossAudience audience, UUID bossInstanceId) {
            events.add("hide:" + bossInstanceId);
        }
    }
}
