package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusModelAnimationPolicyTest {

    @Test
    void usesCriticalAtOrBelowOneQuarterHealth() {
        var policy = new NexusModelAnimationPolicy();

        assertEquals("critical", policy.steadyAnimation(0.25));
        assertEquals("idle", policy.steadyAnimation(0.26));
    }

    @Test
    void usesDamagedAndDestroyedForTransientStates() {
        var policy = new NexusModelAnimationPolicy();

        assertEquals("damaged", policy.damageAnimation());
        assertEquals("destroyed", policy.destroyedAnimation());
    }
}
