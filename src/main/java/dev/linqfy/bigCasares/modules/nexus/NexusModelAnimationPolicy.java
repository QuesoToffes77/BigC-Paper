package dev.linqfy.bigCasares.modules.nexus;

public final class NexusModelAnimationPolicy {

    public String steadyAnimation(double healthFraction) {
        return healthFraction <= 0.25 ? "critical" : "idle";
    }

    public String damageAnimation() {
        return "damaged";
    }

    public String destroyedAnimation() {
        return "destroyed";
    }
}
