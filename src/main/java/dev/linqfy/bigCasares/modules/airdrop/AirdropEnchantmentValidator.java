package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

final class AirdropEnchantmentValidator {
    private AirdropEnchantmentValidator() {
    }

    static void validate() {
        for (String key : AirdropEnchantmentLootGenerator.configuredKeys()) {
            if (Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key)) == null) {
                throw new IllegalStateException("Unknown Airdrop enchantment: minecraft:" + key);
            }
        }
    }
}
