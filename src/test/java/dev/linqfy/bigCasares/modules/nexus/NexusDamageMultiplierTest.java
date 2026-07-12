package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusDamageMultiplierTest {

    @Test
    void appliesConfiguredMultiplierToTheUnmodifiedFinalDamage() {
        NexusId nexusId = NexusId.random();
        NexusDamageMultipliers multipliers = NexusDamageMultipliers.defaults()
                .with(NexusDamageKind.EXPLOSION, 0.5);
        NexusService service = service(nexusId, multipliers, (ignoredNexus, ignoredPlayer) -> false);

        NexusDamageResult result = service.applyDamage(request(nexusId, Optional.empty(), NexusDamageKind.EXPLOSION, 16.0));

        assertEquals(NexusDamageOutcome.APPLIED, result.outcome());
        assertEquals(16.0, result.actualDamage());
        assertEquals(0.5, result.multiplier());
        assertEquals(8.0, result.appliedDamage());
        assertEquals(492.0, result.currentHealth());
    }

    @Test
    void memberCannotDamageOwnTeamNexus() {
        NexusId nexusId = NexusId.random();
        UUID member = UUID.randomUUID();
        NexusService service = service(nexusId, NexusDamageMultipliers.defaults(),
                (testedNexus, player) -> testedNexus.equals(nexusId) && player.equals(member));

        NexusDamageResult result = service.applyDamage(request(
                nexusId,
                Optional.of(member),
                NexusDamageKind.MELEE,
                20.0
        ));

        assertEquals(NexusDamageOutcome.IGNORED_OWN_TEAM, result.outcome());
        assertEquals(0.0, result.appliedDamage());
        assertEquals(500.0, result.currentHealth());
    }

    @Test
    void environmentDamageIsDisabledByDefault() {
        NexusId nexusId = NexusId.random();
        NexusService service = service(nexusId, NexusDamageMultipliers.defaults(),
                (ignoredNexus, ignoredPlayer) -> false);

        NexusDamageResult result = service.applyDamage(request(
                nexusId,
                Optional.empty(),
                NexusDamageKind.ENVIRONMENT,
                40.0
        ));

        assertEquals(NexusDamageOutcome.IGNORED_BY_MULTIPLIER, result.outcome());
        assertEquals(0.0, result.appliedDamage());
        assertEquals(500.0, result.currentHealth());
    }

    private static NexusService service(
            NexusId nexusId,
            NexusDamageMultipliers multipliers,
            NexusTeamGateway teamGateway
    ) {
        NexusService service = new NexusService(
            500.0,
            multipliers,
            teamGateway,
            NexusVisualGateway.noop(),
            new NexusDamagePolicy(true, true),
            ignored -> { }
        );
        service.register(nexusId);
        return service;
    }

    private static NexusDamageRequest request(
            NexusId nexusId,
            Optional<UUID> attacker,
            NexusDamageKind kind,
            double actualDamage
    ) {
        return new NexusDamageRequest(
                nexusId,
                attacker,
                kind,
                actualDamage,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Instant.now()
        );
    }
}
