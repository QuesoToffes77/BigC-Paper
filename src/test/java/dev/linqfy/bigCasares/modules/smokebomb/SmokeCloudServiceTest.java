package dev.linqfy.bigCasares.modules.smokebomb;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmokeCloudServiceTest {

    @Test
    void keepsEntityConcealedInsideCloudAndForTwoSecondsAfterExit() {
        UUID entityId = UUID.randomUUID();
        SmokeBombSettings settings = settings();
        SmokeCloudService service = new SmokeCloudService(settings);

        service.createCloud("cloud-1", 0L);

        SmokeCloudService.TickResult firstTick = service.tick(0L, Set.of(entityId));
        assertEquals(Set.of(entityId), firstTick.newlyConcealed());
        assertEquals(Set.of(entityId), service.getConcealedEntities());

        SmokeCloudService.TickResult exitTick = service.tick(1L, Set.of());
        assertEquals(Set.of(), exitTick.newlyRevealed());
        assertEquals(Set.of(entityId), service.getConcealedEntities());

        service.tick(39L, Set.of());
        assertEquals(Set.of(entityId), service.getConcealedEntities());

        SmokeCloudService.TickResult revealTick = service.tick(41L, Set.of());
        assertEquals(Set.of(entityId), revealTick.newlyRevealed());
        assertEquals(Set.of(), service.getConcealedEntities());
    }

    @Test
    void cancelsPendingRevealWhenEntityReentersBeforeLingerEnds() {
        UUID entityId = UUID.randomUUID();
        SmokeBombSettings settings = settings();
        SmokeCloudService service = new SmokeCloudService(settings);

        service.createCloud("cloud-1", 0L);
        service.tick(0L, Set.of(entityId));
        service.tick(1L, Set.of());

        SmokeCloudService.TickResult reentryTick = service.tick(20L, Set.of(entityId));
        assertEquals(Set.of(), reentryTick.newlyConcealed());
        assertEquals(Set.of(), reentryTick.newlyRevealed());

        service.tick(60L, Set.of(entityId));
        assertEquals(Set.of(entityId), service.getConcealedEntities());
    }

    @Test
    void expiresCloudAfterThirtySecondsButHonorsLinger() {
        UUID entityId = UUID.randomUUID();
        SmokeBombSettings settings = settings();
        SmokeCloudService service = new SmokeCloudService(settings);

        service.createCloud("cloud-1", 0L);
        service.tick(0L, Set.of(entityId));

        SmokeCloudService.TickResult expiryTick = service.tick(600L, Set.of(entityId));
        assertEquals(Set.of(), expiryTick.newlyRevealed());
        assertEquals(Set.of(entityId), service.getConcealedEntities());

        SmokeCloudService.TickResult revealTick = service.tick(641L, Set.of());
        assertEquals(Set.of(entityId), revealTick.newlyRevealed());
    }

    private SmokeBombSettings settings() {
        return new SmokeBombSettings(
            20L * 30,
            12.0,
            12.0,
            5.0,
            40L,
            20L,
            200,
            60,
            32.0,
            5L
        );
    }
}
