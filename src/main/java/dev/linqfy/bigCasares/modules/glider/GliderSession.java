package dev.linqfy.bigCasares.modules.glider;

import java.util.UUID;

public final class GliderSession {

    private final UUID playerId;
    private final UUID worldId;
    private GliderTier tier;
    private final long startTick;
    private final double startY;
    private GliderPhase phase = GliderPhase.GLIDING;
    private int glideTicks;

    GliderSession(UUID playerId, UUID worldId, GliderTier tier, long startTick, double startY) {
        this.playerId = playerId;
        this.worldId = worldId;
        this.tier = tier;
        this.startTick = startTick;
        this.startY = startY;
    }

    public UUID playerId() {
        return playerId;
    }

    public UUID worldId() {
        return worldId;
    }

    public GliderTier tier() {
        return tier;
    }

    void tier(GliderTier tier) {
        this.tier = java.util.Objects.requireNonNull(tier, "tier");
    }

    public long startTick() {
        return startTick;
    }

    public double startY() {
        return startY;
    }

    public GliderPhase phase() {
        return phase;
    }

    public int glideTicks() {
        return glideTicks;
    }

    void tick() {
        glideTicks++;
        phase = GliderPhase.GLIDING;
    }

    void boosting() {
        phase = GliderPhase.BOOSTING;
    }

    void finish(GliderStopReason reason) {
        phase = reason == GliderStopReason.LANDED ? GliderPhase.LANDING : GliderPhase.DONE;
    }
}
