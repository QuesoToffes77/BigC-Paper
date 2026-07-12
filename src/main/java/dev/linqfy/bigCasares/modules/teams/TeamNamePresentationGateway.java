package dev.linqfy.bigCasares.modules.teams;

import java.util.UUID;

public interface TeamNamePresentationGateway {

    void refreshPlayer(UUID playerId, TeamPresentation presentation);

    void clearPlayer(UUID playerId);

    void refreshTeam(TeamId teamId);
}
