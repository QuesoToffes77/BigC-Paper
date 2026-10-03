package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Locale;
import java.util.Objects;

public record AirdropStoredEnchantment(
    String key,
    int level,
    int maxVanillaLevel,
    boolean treasure,
    boolean curse
) {
    public AirdropStoredEnchantment {
        key = Objects.requireNonNull(key, "key").toLowerCase(Locale.ROOT);
        if (level < 1 || maxVanillaLevel < 1 || level > maxVanillaLevel) {
            throw new IllegalArgumentException("enchantment level exceeds vanilla bounds");
        }
    }
}
