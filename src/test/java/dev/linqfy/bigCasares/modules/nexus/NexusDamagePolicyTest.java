package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusDamagePolicyTest {

    @Test
    void naturalEntitiesCannotDamageByDefault() {
        NexusId id = NexusId.random();
        NexusService service = service(id, NexusDamagePolicy.safeDefaults());

        NexusDamageResult result = service.applyDamage(request(
            id, NexusDamageKind.MELEE, NexusAttackerOrigin.NATURAL_ENTITY));

        assertEquals(NexusDamageOutcome.IGNORED_NATURAL_ENTITY, result.outcome());
        assertEquals(500.0, result.currentHealth());
    }

    @Test
    void unownedExplosionsAreConfigurableAndDisabledByDefault() {
        NexusId id = NexusId.random();
        NexusService service = service(id, NexusDamagePolicy.safeDefaults());

        NexusDamageResult result = service.applyDamage(request(
            id, NexusDamageKind.EXPLOSION, NexusAttackerOrigin.UNOWNED));

        assertEquals(NexusDamageOutcome.IGNORED_UNOWNED_EXPLOSION, result.outcome());
    }

    @Test
    void explicitlyEnabledNaturalAndUnownedDamageUsesNormalMultipliers() {
        NexusId id = NexusId.random();
        NexusService service = service(id, new NexusDamagePolicy(true, true));

        NexusDamageResult natural = service.applyDamage(request(
            id, NexusDamageKind.MELEE, NexusAttackerOrigin.NATURAL_ENTITY));
        NexusDamageResult explosion = service.applyDamage(request(
            id, NexusDamageKind.EXPLOSION, NexusAttackerOrigin.UNOWNED));

        assertEquals(NexusDamageOutcome.APPLIED, natural.outcome());
        assertEquals(NexusDamageOutcome.APPLIED, explosion.outcome());
        assertEquals(480.0, explosion.currentHealth());
    }

    private static NexusService service(NexusId id, NexusDamagePolicy policy) {
        NexusService service = new NexusService(
            500.0,
            NexusDamageMultipliers.defaults(),
            (ignoredNexus, ignoredPlayer) -> false,
            NexusVisualGateway.noop(),
            policy,
            ignored -> { }
        );
        service.register(id);
        return service;
    }

    private static NexusDamageRequest request(
        NexusId id,
        NexusDamageKind kind,
        NexusAttackerOrigin origin
    ) {
        return new NexusDamageRequest(
            id,
            Optional.of(UUID.randomUUID()),
            kind,
            10.0,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Instant.parse("2026-07-11T00:00:00Z"),
            origin
        );
    }
}
