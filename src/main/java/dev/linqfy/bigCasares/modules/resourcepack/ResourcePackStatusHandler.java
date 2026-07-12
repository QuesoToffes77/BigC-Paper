package dev.linqfy.bigCasares.modules.resourcepack;

import java.util.UUID;

final class ResourcePackStatusHandler {
    private final ResourcePackService service;

    ResourcePackStatusHandler(ResourcePackService service) {
        this.service = service;
    }

    void handle(UUID packId, UUID playerId, String statusName, Runnable requiredPackFailure) {
        if (!service.isBigCasaresPack(packId)) {
            return;
        }
        ResourcePackPlayerState state = map(statusName);
        service.updateState(playerId, state);
        if (service.isRequired() && (state == ResourcePackPlayerState.DECLINED
            || state == ResourcePackPlayerState.FAILED
            || state == ResourcePackPlayerState.DISCARDED)) {
            requiredPackFailure.run();
        }
    }

    static ResourcePackPlayerState map(String statusName) {
        return switch (statusName) {
            case "ACCEPTED" -> ResourcePackPlayerState.ACCEPTED;
            case "DOWNLOADED" -> ResourcePackPlayerState.DOWNLOADED;
            case "SUCCESSFULLY_LOADED" -> ResourcePackPlayerState.LOADED;
            case "DECLINED" -> ResourcePackPlayerState.DECLINED;
            case "DISCARDED" -> ResourcePackPlayerState.DISCARDED;
            case "FAILED_DOWNLOAD", "FAILED_RELOAD", "INVALID_URL" -> ResourcePackPlayerState.FAILED;
            default -> ResourcePackPlayerState.SENT;
        };
    }
}
