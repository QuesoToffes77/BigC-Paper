package dev.linqfy.bigCasares.modules.nexus;

import dev.linqfy.bigCasares.modules.teams.TeamId;
import java.util.UUID;

public record NexusProtectedContainer(NexusId nexusId, TeamId teamId, UUID worldId, int x, int y, int z) {
    public boolean matches(UUID world, int bx, int by, int bz) {
        return worldId.equals(world) && x == bx && y == by && z == bz;
    }
}
