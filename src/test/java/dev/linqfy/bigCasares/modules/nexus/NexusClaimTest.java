package dev.linqfy.bigCasares.modules.nexus;

import dev.linqfy.bigCasares.modules.teams.TeamId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusClaimTest {

    private final NexusPlacementPolicy policy = new NexusPlacementPolicy(NexusPlacementSettings.defaults());

    @Test
    void detectsOverlapWithinProtectedVolume() {
        NexusBlockPosition nexus = new NexusBlockPosition(100, 64, 100);

        // Same position
        assertTrue(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(100, 64, 100)));

        // Within horizontal boundary (4 blocks) and vertical (3 blocks)
        assertTrue(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(104, 67, 104)));
        assertTrue(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(96, 61, 96)));

        // Just outside horizontal boundary
        assertFalse(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(105, 64, 100)));
        assertFalse(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(100, 64, 105)));

        // Just outside vertical boundary
        assertFalse(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(100, 68, 100)));
        assertFalse(policy.isInsideProtectedVolume(nexus, new NexusBlockPosition(100, 60, 100)));
    }

    @Test
    void verifiesTeamlessTeamIdIsConstantAndDead() {
        TeamId teamless = new TeamId(new UUID(0, 0));
        assertTrue(teamless.value().equals(new UUID(0, 0)));
        // A player belongs to some real UUID team, which will never equal all-zeros UUID
        TeamId playerTeam = new TeamId(UUID.randomUUID());
        assertFalse(playerTeam.equals(teamless));
    }
}
