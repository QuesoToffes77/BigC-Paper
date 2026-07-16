package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PaymentPaper {

    private final NamespacedKey voucherIdKey;

    public PaymentPaper(JavaPlugin plugin) {
        this.voucherIdKey = new NamespacedKey(plugin, "payment-voucher-id");
    }

    public ItemStack create(PaymentVoucher voucher, String formattedAmount) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Vale de pago: " + formattedAmount);
        meta.setLore(List.of(
                ChatColor.GRAY + "Clic derecho para cobrar.",
                ChatColor.DARK_GRAY + "Dáselo a quien deba cobrarlo."
        ));
        meta.getPersistentDataContainer().set(voucherIdKey, PersistentDataType.STRING, voucher.id().toString());
        paper.setItemMeta(meta);
        return paper;
    }

    public Optional<UUID> voucherId(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String rawId = item.getItemMeta().getPersistentDataContainer().get(voucherIdKey, PersistentDataType.STRING);
        if (rawId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(rawId));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
