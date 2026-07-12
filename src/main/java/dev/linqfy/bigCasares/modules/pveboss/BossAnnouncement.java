package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;
import java.util.UUID;

public record BossAnnouncement(
    UUID bossInstanceId,
    BossAnnouncementChannel channel,
    String message
) {

    public BossAnnouncement {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        Objects.requireNonNull(channel, "channel");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("announcement message must not be blank");
        }
    }
}
