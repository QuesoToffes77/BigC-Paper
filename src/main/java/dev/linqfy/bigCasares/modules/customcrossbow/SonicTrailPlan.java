package dev.linqfy.bigCasares.modules.customcrossbow;

public record SonicTrailPlan(int ticks, int pointsPerTick, double spacing) {

    public static SonicTrailPlan create(double beamRange) {
        int ticks = 5;
        int pointsPerTick = (int) Math.max(1, Math.min(8, Math.ceil(beamRange / 5.0)));
        double spacing = beamRange / (ticks * pointsPerTick);
        return new SonicTrailPlan(ticks, pointsPerTick, spacing);
    }

    public int totalParticlePoints() {
        return ticks * pointsPerTick;
    }
}
