package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pure grapple-session simulation. One session per player with four phases:
 *
 * <ul>
 *   <li>{@link Phase#TRAVEL}: the hook flies from the eye toward the target
 *       while the chain extends behind it. Re-activating starts a retract.</li>
 *   <li>{@link Phase#ATTACH}: the hook is fixed on the target and the chain
 *       stays tense between the player's eye and the target every tick; the
 *       first attach tick carries the pull impulse. The session ends when the
 *       player arrives (or, for a miss, after the brief hold) and is always
 *       capped so no session can run forever.</li>
 *   <li>{@link Phase#RETRACT}: the hook returns to the player and the chain
 *       disappears progressively, then the session ends without a pull.</li>
 *   <li>{@link Phase#DONE}: the final tick; the runtime cleans up.</li>
 * </ul>
 *
 * No Bukkit types beyond {@link Vector} are used, so phase transitions, chain
 * growth and the pull are fully unit-testable.
 */
public final class GrappleSimulation {

    public enum Phase {
        TRAVEL,
        ATTACH,
        RETRACT,
        DONE
    }

    /**
     * Longest an attach may last (ticks). The chain stays tense while the
     * player is pulled and drops early on arrival, so this only bounds the
     * case where the player never reaches the target.
     */
    public static final int MAX_ATTACH_TICKS = 100;

    /**
     * The player counts as having arrived (and the swing ends) when their eye
     * is within this many blocks of the live target. Generous enough for
     * floor/ceiling shots, where the eye can never touch the attach point.
     */
    public static final double ARRIVAL_DISTANCE = 2.0;

    /**
     * One simulation step. {@code impulseVelocity} is non-null exactly on the
     * tick the hook arrives (the pull moment); {@code done} is true only on
     * the final tick.
     */
    public record GrappleTick(
        Phase phase,
        Vector hookPosition,
        List<Vector> chainPositions,
        Vector impulseVelocity,
        boolean done
    ) {
        public GrappleTick {
            hookPosition = hookPosition.clone();
            chainPositions = List.copyOf(chainPositions);
            impulseVelocity = impulseVelocity == null ? null : impulseVelocity.clone();
        }
    }

    public static final class GrappleSession {

        private final UUID ownerId;
        private final GrapplingHookTier tier;
        private final Vector start;
        private final Vector target;
        private final int travelTicks;
        private final int attachTicks;
        private final double chainSpacing;
        private final double impulsePower;
        private final double upwardBias;
        private final double maxImpulseSpeed;
        private final boolean pullEnabled;
        private final double retractSpeedMultiplier;
        private Phase phase = Phase.TRAVEL;
        private int elapsedTicks;
        private Vector lastHookPosition;
        private Vector retractOrigin;
        private int retractTicks;

        GrappleSession(
            UUID ownerId,
            GrapplingHookTier tier,
            Vector start,
            Vector target,
            int travelTicks,
            int attachTicks,
            double chainSpacing,
            double impulsePower,
            double upwardBias,
            double maxImpulseSpeed,
            boolean pullEnabled
        ) {
            this(ownerId, tier, start, target, travelTicks, attachTicks, chainSpacing,
                impulsePower, upwardBias, maxImpulseSpeed, pullEnabled, 1.0);
        }

        GrappleSession(
            UUID ownerId,
            GrapplingHookTier tier,
            Vector start,
            Vector target,
            int travelTicks,
            int attachTicks,
            double chainSpacing,
            double impulsePower,
            double upwardBias,
            double maxImpulseSpeed,
            boolean pullEnabled,
            double retractSpeedMultiplier
        ) {
            this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
            this.tier = Objects.requireNonNull(tier, "tier");
            this.start = start.clone();
            this.target = target.clone();
            this.travelTicks = Math.max(1, travelTicks);
            this.attachTicks = Math.max(0, attachTicks);
            this.chainSpacing = chainSpacing;
            this.impulsePower = impulsePower;
            this.upwardBias = upwardBias;
            this.maxImpulseSpeed = Math.max(0.0, maxImpulseSpeed);
            this.pullEnabled = pullEnabled;
            this.retractSpeedMultiplier = Math.max(0.25, retractSpeedMultiplier);
            this.lastHookPosition = start.clone();
        }

        /**
         * Advances the session by one tick against the stored (fixed) target.
         * Equivalent to {@link #advance(Vector, Vector)} with the target that
         * was captured when the shot was fired.
         */
        public GrappleTick advance(Vector anchor) {
            return advance(anchor, target);
        }

        /**
         * Advances the session by one tick. {@code anchor} is the player's
         * current eye position; the chain is always sampled from it so the
         * rope follows a moving player. {@code liveTarget} is the current
         * anchor point of the shot: for a block shot it stays the captured
         * target, for an entity shot the runtime passes the entity's current
         * position so the hook, chain and pull all track the target.
         */
        public GrappleTick advance(Vector anchor, Vector liveTarget) {
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(liveTarget, "liveTarget");
            elapsedTicks++;
            GrappleTick tick;
            switch (phase) {
                case TRAVEL -> {
                    if (elapsedTicks >= travelTicks) {
                        phase = Phase.ATTACH;
                        elapsedTicks = 0;
                        // Hook arrives: chain goes tense and the pull fires.
                        Vector impulse = pullEnabled
                            ? GrappleShotMath.impulseVelocity(anchor, liveTarget, impulsePower, upwardBias,
                                maxImpulseSpeed)
                            : null;
                        tick = new GrappleTick(Phase.ATTACH, liveTarget.clone(),
                            GrappleShotMath.chainPositions(anchor, liveTarget, chainSpacing), impulse, false);
                    } else {
                        tick = tick(anchor, liveTarget);
                    }
                }
                case ATTACH -> {
                    boolean arrived = anchor.distanceSquared(liveTarget)
                        <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE;
                    if (elapsedTicks >= attachTicks && (!pullEnabled || arrived)) {
                        phase = Phase.DONE;
                        tick = done(anchor, liveTarget);
                    } else if (elapsedTicks >= MAX_ATTACH_TICKS) {
                        phase = Phase.DONE;
                        tick = done(anchor, liveTarget);
                    } else {
                        tick = new GrappleTick(Phase.ATTACH, liveTarget.clone(),
                            GrappleShotMath.chainPositions(anchor, liveTarget, chainSpacing), null, false);
                    }
                }
                case RETRACT -> {
                    if (elapsedTicks >= retractTicks) {
                        phase = Phase.DONE;
                        tick = new GrappleTick(Phase.DONE, anchor.clone(), List.of(), null, true);
                    } else {
                        Vector hook = GrappleShotMath.lerp(retractOrigin, anchor,
                            (double) elapsedTicks / retractTicks);
                        tick = new GrappleTick(Phase.RETRACT, hook,
                            GrappleShotMath.chainPositions(anchor, hook, chainSpacing), null, false);
                    }
                }
                default -> throw new IllegalStateException("Grapple session already finished");
            }
            lastHookPosition = tick.hookPosition();
            return tick;
        }

        /**
         * Starts retracting the hook back to the player. Only meaningful while
         * the hook is in flight or attached; a second call is ignored, so a
         * repeated activation can never create a second session.
         */
        public void beginRetract() {
            if (phase == Phase.TRAVEL || phase == Phase.ATTACH) {
                retractOrigin = lastHookPosition != null ? lastHookPosition : target.clone();
                phase = Phase.RETRACT;
                elapsedTicks = 0;
                retractTicks = Math.max(1, (int) Math.ceil(travelTicks / retractSpeedMultiplier));
            }
        }

        public Phase phase() {
            return phase;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public GrapplingHookTier tier() {
            return tier;
        }

        public Vector start() {
            return start.clone();
        }

        public Vector target() {
            return target.clone();
        }

        public int travelTicks() {
            return travelTicks;
        }

        private GrappleTick tick(Vector anchor, Vector liveTarget) {
            Vector hook = GrappleShotMath.lerp(start, liveTarget, (double) elapsedTicks / travelTicks);
            return new GrappleTick(phase, hook, GrappleShotMath.chainPositions(anchor, hook, chainSpacing),
                null, false);
        }

        private GrappleTick done(Vector anchor, Vector liveTarget) {
            return new GrappleTick(Phase.DONE, liveTarget.clone(),
                GrappleShotMath.chainPositions(anchor, liveTarget, chainSpacing), null, true);
        }
    }
}
