package dev.linqfy.bigCasares.modules.nexus;

import java.util.UUID;

@FunctionalInterface
public interface NexusTeamGateway {

    boolean isMemberOfOwningTeam(NexusId nexusId, UUID playerId);

    static NexusTeamGateway noTeams() {
        return (nexusId, playerId) -> false;
    }
}
