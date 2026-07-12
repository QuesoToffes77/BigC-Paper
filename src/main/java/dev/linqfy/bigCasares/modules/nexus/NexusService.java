package dev.linqfy.bigCasares.modules.nexus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class NexusService {

    private final double maximumHealth;
    private final NexusDamageMultipliers multipliers;
    private final NexusTeamGateway teamGateway;
    private final NexusVisualGateway visualGateway;
    private final NexusDamagePolicy damagePolicy;
    private final Consumer<NexusSnapshot> stateChanged;
    private final Map<NexusId, MutableNexus> nexuses = new LinkedHashMap<>();

    public NexusService(
            double maximumHealth,
            NexusDamageMultipliers multipliers,
            NexusTeamGateway teamGateway,
            NexusVisualGateway visualGateway
    ) {
        this(
            maximumHealth,
            multipliers,
            teamGateway,
            visualGateway,
            NexusDamagePolicy.safeDefaults(),
            ignored -> { }
        );
    }

    public NexusService(
            double maximumHealth,
            NexusDamageMultipliers multipliers,
            NexusTeamGateway teamGateway,
            NexusVisualGateway visualGateway,
            Consumer<NexusSnapshot> stateChanged
    ) {
        this(
            maximumHealth,
            multipliers,
            teamGateway,
            visualGateway,
            NexusDamagePolicy.safeDefaults(),
            stateChanged
        );
    }

    public NexusService(
            double maximumHealth,
            NexusDamageMultipliers multipliers,
            NexusTeamGateway teamGateway,
            NexusVisualGateway visualGateway,
            NexusDamagePolicy damagePolicy,
            Consumer<NexusSnapshot> stateChanged
    ) {
        if (!Double.isFinite(maximumHealth) || maximumHealth <= 0.0) {
            throw new IllegalArgumentException("maximumHealth must be finite and positive");
        }
        this.maximumHealth = maximumHealth;
        this.multipliers = Objects.requireNonNull(multipliers, "multipliers");
        this.teamGateway = Objects.requireNonNull(teamGateway, "teamGateway");
        this.visualGateway = Objects.requireNonNull(visualGateway, "visualGateway");
        this.damagePolicy = Objects.requireNonNull(damagePolicy, "damagePolicy");
        this.stateChanged = Objects.requireNonNull(stateChanged, "stateChanged");
    }

    public synchronized NexusSnapshot register(NexusId nexusId) {
        Objects.requireNonNull(nexusId, "nexusId");
        MutableNexus nexus = new MutableNexus(maximumHealth, Optional.empty());
        nexuses.put(nexusId, nexus);
        return snapshot(nexusId, nexus);
    }

    public synchronized NexusVisualHandle register(NexusVisualRequest request) {
        Objects.requireNonNull(request, "request");
        MutableNexus nexus = new MutableNexus(request.currentHealth(), Optional.of(request.position()));
        nexuses.put(request.nexusId(), nexus);
        return visualGateway.spawn(request);
    }

    public synchronized NexusSnapshot restore(NexusVisualRequest request) {
        Objects.requireNonNull(request, "request");
        MutableNexus nexus = new MutableNexus(request.currentHealth(), Optional.of(request.position()));
        nexuses.put(request.nexusId(), nexus);
        return snapshot(request.nexusId(), nexus);
    }

    public synchronized NexusDamageResult applyDamage(NexusDamageRequest request) {
        Objects.requireNonNull(request, "request");
        MutableNexus nexus = nexuses.get(request.nexusId());
        if (nexus == null) {
            return new NexusDamageResult(
                    NexusDamageOutcome.NOT_FOUND,
                    request.actualDamage(),
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }

        double previousHealth = nexus.health;
        Optional<NexusDamageOutcome> policyRejection = damagePolicy.rejectionFor(request);
        if (policyRejection.isPresent()) {
            return ignored(request, policyRejection.orElseThrow(), previousHealth, 0.0);
        }
        if (request.attackerId().isPresent()
                && teamGateway.isMemberOfOwningTeam(request.nexusId(), request.attackerId().orElseThrow())) {
            return ignored(request, NexusDamageOutcome.IGNORED_OWN_TEAM, previousHealth, 0.0);
        }

        double multiplier = multipliers.forKind(request.kind());
        if (multiplier == 0.0 || request.actualDamage() == 0.0) {
            return ignored(request, NexusDamageOutcome.IGNORED_BY_MULTIPLIER, previousHealth, multiplier);
        }

        double appliedDamage = request.actualDamage() * multiplier;
        nexus.health = Math.max(0.0, previousHealth - appliedDamage);
        stateChanged.accept(snapshot(request.nexusId(), nexus));
        double fraction = nexus.health / maximumHealth;
        visualGateway.updateHealth(request.nexusId(), fraction);
        visualGateway.playDamageAnimation(request.nexusId(), request.kind());

        NexusDamageOutcome outcome = nexus.health == 0.0
                ? NexusDamageOutcome.DESTROYED
                : NexusDamageOutcome.APPLIED;
        if (outcome == NexusDamageOutcome.DESTROYED) {
            visualGateway.playDestroyedAnimation(request.nexusId());
        }

        return new NexusDamageResult(
                outcome,
                request.actualDamage(),
                multiplier,
                appliedDamage,
                previousHealth,
                nexus.health
        );
    }

    public synchronized Optional<NexusSnapshot> find(NexusId nexusId) {
        MutableNexus nexus = nexuses.get(Objects.requireNonNull(nexusId, "nexusId"));
        return nexus == null ? Optional.empty() : Optional.of(snapshot(nexusId, nexus));
    }

    public synchronized List<NexusPosition> activePositions() {
        return nexuses.entrySet().stream()
                .filter(entry -> entry.getValue().health > 0.0)
                .map(Map.Entry::getValue)
                .map(MutableNexus::position)
                .flatMap(Optional::stream)
                .toList();
    }

    public synchronized void remove(NexusId nexusId) {
        if (nexuses.remove(Objects.requireNonNull(nexusId, "nexusId")) != null) {
            visualGateway.remove(nexusId);
        }
    }

    private NexusDamageResult ignored(
            NexusDamageRequest request,
            NexusDamageOutcome outcome,
            double health,
            double multiplier
    ) {
        return new NexusDamageResult(outcome, request.actualDamage(), multiplier, 0.0, health, health);
    }

    private NexusSnapshot snapshot(NexusId nexusId, MutableNexus nexus) {
        return new NexusSnapshot(nexusId, nexus.health, maximumHealth, nexus.position);
    }

    private static final class MutableNexus {
        private double health;
        private final Optional<NexusPosition> position;

        private MutableNexus(double health, Optional<NexusPosition> position) {
            this.health = health;
            this.position = position;
        }

        private Optional<NexusPosition> position() {
            return position;
        }
    }
}
