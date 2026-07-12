package dev.linqfy.bigCasares.modules.nexus;

import java.util.Objects;
import java.util.Optional;

public record NexusDamagePolicy(
    boolean allowNaturalEntities,
    boolean allowUnownedExplosions
) {
    public static NexusDamagePolicy safeDefaults() {
        return new NexusDamagePolicy(false, false);
    }

    public Optional<NexusDamageOutcome> rejectionFor(NexusDamageRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.attackerOrigin() == NexusAttackerOrigin.NATURAL_ENTITY
            && !allowNaturalEntities) {
            return Optional.of(NexusDamageOutcome.IGNORED_NATURAL_ENTITY);
        }
        if (request.kind() == NexusDamageKind.EXPLOSION
            && request.attackerOrigin() == NexusAttackerOrigin.UNOWNED
            && !allowUnownedExplosions) {
            return Optional.of(NexusDamageOutcome.IGNORED_UNOWNED_EXPLOSION);
        }
        return Optional.empty();
    }
}
