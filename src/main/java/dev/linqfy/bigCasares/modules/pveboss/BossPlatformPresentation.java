package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossPlatformPresentation(
    BossPresentationMode mode,
    String entityPresentation,
    String bossBarIconKey,
    String soundKey,
    boolean customAssets
) {

    public BossPlatformPresentation {
        Objects.requireNonNull(mode, "mode");
        if (entityPresentation == null || entityPresentation.isBlank()) {
            throw new IllegalArgumentException("entityPresentation must not be blank");
        }
        Objects.requireNonNull(bossBarIconKey, "bossBarIconKey");
        if (soundKey == null || soundKey.isBlank()) {
            throw new IllegalArgumentException("soundKey must not be blank");
        }
    }
}
