package dev.linqfy.bigCasares.modules.airdrop;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record AirdropReward(
    AirdropRewardKind kind,
    String itemId,
    int amount,
    AirdropQuality quality,
    List<AirdropStoredEnchantment> enchantments
) {
    public AirdropReward {
        kind = Objects.requireNonNull(kind, "kind");
        itemId = Objects.requireNonNull(itemId, "itemId").toLowerCase(Locale.ROOT);
        quality = Objects.requireNonNull(quality, "quality");
        enchantments = List.copyOf(Objects.requireNonNull(enchantments, "enchantments"));
        if (amount < 1) {
            throw new IllegalArgumentException("reward amount must be positive");
        }
        if (kind == AirdropRewardKind.ENCHANTED_BOOK && enchantments.isEmpty()) {
            throw new IllegalArgumentException("enchanted books require at least one enchantment");
        }
        if (kind != AirdropRewardKind.ENCHANTED_BOOK && !enchantments.isEmpty()) {
            throw new IllegalArgumentException("only enchanted books may contain stored enchantments");
        }
    }

    public static AirdropReward material(String material, int amount, AirdropQuality quality) {
        return new AirdropReward(AirdropRewardKind.MATERIAL, material, amount, quality, List.of());
    }

    public static AirdropReward customItem(String id, int amount, AirdropQuality quality) {
        return new AirdropReward(AirdropRewardKind.CUSTOM_ITEM, id, amount, quality, List.of());
    }

    public static AirdropReward book(AirdropQuality quality, List<AirdropStoredEnchantment> enchantments) {
        return new AirdropReward(AirdropRewardKind.ENCHANTED_BOOK, "enchanted_book", 1, quality, enchantments);
    }

    public boolean isCustomItem() {
        return kind == AirdropRewardKind.CUSTOM_ITEM;
    }

    public boolean isEnchantedBook() {
        return kind == AirdropRewardKind.ENCHANTED_BOOK;
    }
}
