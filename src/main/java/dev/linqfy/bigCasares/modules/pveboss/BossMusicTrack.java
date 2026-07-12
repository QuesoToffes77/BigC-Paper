package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

public record BossMusicTrack(
    String id,
    String customSoundKey,
    String fallbackSoundKey,
    BossMusicSource source
) {

    public BossMusicTrack(String id, String customSoundKey, String fallbackSoundKey) {
        this(id, customSoundKey, fallbackSoundKey, BossMusicSource.AUTO);
    }

    public BossMusicTrack {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("music track id must not be blank");
        }
        Objects.requireNonNull(customSoundKey, "customSoundKey");
        if (fallbackSoundKey == null || fallbackSoundKey.isBlank()) {
            throw new IllegalArgumentException("fallbackSoundKey must not be blank");
        }
        Objects.requireNonNull(source, "source");
        if (source == BossMusicSource.CUSTOM && customSoundKey.isBlank()) {
            throw new IllegalArgumentException("a custom track requires customSoundKey");
        }
    }

    public BossMusicTrack resolve(boolean customAvailable) {
        if (source != BossMusicSource.AUTO) {
            return this;
        }
        BossMusicSource resolved = customAvailable && !customSoundKey.isBlank()
            ? BossMusicSource.CUSTOM
            : BossMusicSource.FALLBACK;
        return new BossMusicTrack(id, customSoundKey, fallbackSoundKey, resolved);
    }

    public String playbackSoundKey() {
        return source == BossMusicSource.FALLBACK || customSoundKey.isBlank()
            ? fallbackSoundKey
            : customSoundKey;
    }
}
