package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NexusDamagePersistenceCallbackTest {

    @Test
    void publishesTheUpdatedSnapshotAfterAppliedDamage() {
        NexusId id = NexusId.random();
        AtomicReference<NexusSnapshot> persisted = new AtomicReference<>();
        NexusService service = new NexusService(
            500.0,
            NexusDamageMultipliers.defaults(),
            (ignoredNexus, ignoredPlayer) -> false,
            NexusVisualGateway.noop(),
            persisted::set
        );
        service.register(id);

        service.applyDamage(request(id, NexusDamageKind.MELEE, 25.0));

        assertEquals(id, persisted.get().nexusId());
        assertEquals(475.0, persisted.get().currentHealth());
    }

    @Test
    void doesNotPersistDamageIgnoredByConfiguration() {
        NexusId id = NexusId.random();
        AtomicReference<NexusSnapshot> persisted = new AtomicReference<>();
        NexusService service = new NexusService(
            500.0,
            NexusDamageMultipliers.defaults(),
            (ignoredNexus, ignoredPlayer) -> false,
            NexusVisualGateway.noop(),
            persisted::set
        );
        service.register(id);

        service.applyDamage(request(id, NexusDamageKind.ENVIRONMENT, 25.0));

        assertNull(persisted.get());
    }

    private static NexusDamageRequest request(NexusId id, NexusDamageKind kind, double damage) {
        return new NexusDamageRequest(
            id,
            Optional.empty(),
            kind,
            damage,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Instant.parse("2026-07-11T00:00:00Z")
        );
    }
}
