package dev.linqfy.bigCasares.modules.nexus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record NexusDamageRequest(
        NexusId nexusId,
        Optional<UUID> attackerId,
        NexusDamageKind kind,
        double actualDamage,
        Optional<String> weaponId,
        Optional<String> damageTypeId,
        Optional<String> directEntityType,
        Instant occurredAt,
        NexusAttackerOrigin attackerOrigin
) {

    public NexusDamageRequest(
        NexusId nexusId,
        Optional<UUID> attackerId,
        NexusDamageKind kind,
        double actualDamage,
        Optional<String> weaponId,
        Optional<String> damageTypeId,
        Optional<String> directEntityType,
        Instant occurredAt
    ) {
        this(
            nexusId,
            attackerId,
            kind,
            actualDamage,
            weaponId,
            damageTypeId,
            directEntityType,
            occurredAt,
            attackerId.isPresent() ? NexusAttackerOrigin.UNKNOWN : NexusAttackerOrigin.UNOWNED
        );
    }

    public NexusDamageRequest {
        Objects.requireNonNull(nexusId, "nexusId");
        attackerId = Objects.requireNonNull(attackerId, "attackerId");
        Objects.requireNonNull(kind, "kind");
        weaponId = Objects.requireNonNull(weaponId, "weaponId");
        damageTypeId = Objects.requireNonNull(damageTypeId, "damageTypeId");
        directEntityType = Objects.requireNonNull(directEntityType, "directEntityType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(attackerOrigin, "attackerOrigin");
        if (!Double.isFinite(actualDamage) || actualDamage < 0.0) {
            throw new IllegalArgumentException("actualDamage must be finite and non-negative");
        }
    }
}
