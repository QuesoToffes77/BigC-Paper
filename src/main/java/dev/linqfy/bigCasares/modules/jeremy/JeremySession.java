package dev.linqfy.bigCasares.modules.jeremy;

import java.util.UUID;

final class JeremySession {
    private final UUID targetUuid;
    private UUID entityUuid;
    private double powerScore;
    private double meleeDamage;
    private long nextPowerRecalculationTick;
    private long lastJeremyDamageMillis = Long.MIN_VALUE;
    private long farSinceTick = -1L;
    private float celebrationYaw;
    private final JeremyStuckDetector stuckDetector;
    private final JeremyUltrasoundController ultrasound;

    JeremySession(
        UUID targetUuid,
        UUID entityUuid,
        double powerScore,
        double meleeDamage,
        JeremyPathfindingSettings pathfinding,
        JeremyUltrasoundSettings ultrasound
    ) {
        this.targetUuid = targetUuid;
        this.entityUuid = entityUuid;
        this.powerScore = powerScore;
        this.meleeDamage = meleeDamage;
        this.stuckDetector = new JeremyStuckDetector(
            pathfinding.stuckDetectionTicks(), pathfinding.minimumProgress(), 4.0);
        this.ultrasound = new JeremyUltrasoundController(ultrasound.chargeTicks(), ultrasound.cooldownTicks());
    }

    UUID targetUuid() { return targetUuid; }
    UUID entityUuid() { return entityUuid; }
    void entityUuid(UUID value) { entityUuid = value; }
    double powerScore() { return powerScore; }
    void powerScore(double value) { powerScore = value; }
    double meleeDamage() { return meleeDamage; }
    void meleeDamage(double value) { meleeDamage = value; }
    long nextPowerRecalculationTick() { return nextPowerRecalculationTick; }
    void nextPowerRecalculationTick(long value) { nextPowerRecalculationTick = value; }
    long lastJeremyDamageMillis() { return lastJeremyDamageMillis; }
    void lastJeremyDamageMillis(long value) { lastJeremyDamageMillis = value; }
    long farSinceTick() { return farSinceTick; }
    void farSinceTick(long value) { farSinceTick = value; }
    float celebrationYaw() { return celebrationYaw; }
    void celebrationYaw(float value) { celebrationYaw = value; }
    JeremyStuckDetector stuckDetector() { return stuckDetector; }
    JeremyUltrasoundController ultrasound() { return ultrasound; }
}
