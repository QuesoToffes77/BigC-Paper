package dev.linqfy.bigCasares.modules.auctionhouse;

import org.bukkit.Server;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import dev.linqfy.bigCasares.module.runtime.PlayerDataCheckpoint;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** Player-facing actions of the auction house shared by the command and the menus. */
final class AuctionHouseController {

    private final Server server;
    private final AuctionHouseService<ItemStack> service;
    private final AuctionMenus menus;
    private final NamespacedKey receiptKey;
    private final NamespacedKey listingReceiptKey;

    AuctionHouseController(Plugin plugin, Server server, AuctionHouseService<ItemStack> service, AuctionMenus menus) {
        this.server = server;
        this.service = service;
        this.menus = menus;
        this.receiptKey = new NamespacedKey(plugin, "auction_claim_receipt");
        this.listingReceiptKey = new NamespacedKey(plugin, "auction_listing_receipt");
    }

    AuctionHouseService<ItemStack> service() {
        return service;
    }

    AuctionMenus menus() {
        return menus;
    }

    void sell(Player player, long price) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType().isAir() || hand.getAmount() <= 0) {
            player.sendMessage("§cTenés que tener en la mano el item que querés vender.");
            return;
        }
        ItemStack item = hand.clone();
        if (service.pendingListing(player.getUniqueId()).isPresent()) {
            player.sendMessage("§cTenés una publicación pendiente de revisión. Contactá a un administrador.");
            return;
        }
        AuctionHouseService.ListingPreparation prepared = service.prepareListing(
            player.getUniqueId(), player.getName(), item, price, inventorySnapshot(player));
        switch (prepared.status()) {
            case LISTED -> {
                try {
                    player.getInventory().setItemInMainHand(null);
                    service.recordListingOutcome(prepared.id(), inventorySnapshot(player));
                    String receipt = service.pendingListing(player.getUniqueId()).orElseThrow().getValue().receiptId();
                    PlayerDataCheckpoint.save(player, listingReceiptKey, receipt);
                    service.finishListing(prepared.id());
                } catch (RuntimeException | java.io.IOException failure) {
                    server.getLogger().log(java.util.logging.Level.SEVERE,
                        "[AuctionHouse] Listing deposit needs review for " + player.getUniqueId(), failure);
                    player.sendMessage("§cTu publicación quedó pendiente de revisión. Contactá a un administrador.");
                    return;
                }
                long tax = service.settings().taxOf(price);
                player.sendMessage("§aPublicaste §f" + item.getAmount() + "x " + describe(item)
                    + " §apor §f" + AuctionFormat.money(price) + "§a."
                    + (tax > 0 ? " §7(Impuesto al vender: " + AuctionFormat.money(tax) + ")" : ""));
            }
            case PRICE_TOO_LOW -> player.sendMessage("§cEl precio mínimo es "
                + AuctionFormat.money(service.settings().minPrice()) + ".");
            case PRICE_TOO_HIGH -> player.sendMessage("§cEl precio máximo es "
                + AuctionFormat.money(service.settings().maxPrice()) + ".");
            case TOO_MANY_LISTINGS -> player.sendMessage("§cYa tenés "
                + service.settings().maxListingsPerPlayer() + " subastas publicadas. Retirá alguna con /ah mis.");
        }
    }

    void buy(Player player, long listingId) {
        AuctionHouseService.BuyResult<ItemStack> result = service.buy(player.getUniqueId(), listingId);
        AuctionListing<ItemStack> listing = result.listing();
        switch (result.status()) {
            case BOUGHT -> {
                player.sendMessage("§aCompraste §f" + listing.item().getAmount() + "x " + describe(listing.item())
                    + " §apor §f" + AuctionFormat.money(listing.price()) + "§a.");
                claim(player);
                Player seller = server.getPlayer(listing.seller());
                if (seller != null) {
                    long payout = listing.price() - service.settings().taxOf(listing.price());
                    seller.sendMessage("§a" + player.getName() + " compró tu " + describe(listing.item())
                        + ". Recibiste §f" + AuctionFormat.money(payout) + "§a.");
                }
            }
            case NOT_FOUND -> player.sendMessage("§cEsa subasta ya no está disponible.");
            case OWN_LISTING -> player.sendMessage("§cNo podés comprar tu propia subasta.");
            case INSUFFICIENT_FUNDS -> player.sendMessage("§cNo tenés suficiente dinero. Cuesta "
                + AuctionFormat.money(listing.price()) + ".");
            case PAYMENT_PENDING -> player.sendMessage("§cEl pago quedó pendiente de revisión. Contactá a un administrador.");
            case PAYMENT_FAILED -> player.sendMessage("§cNo se pudo completar el pago. Intentá de nuevo.");
        }
    }

    void cancel(Player player, long listingId) {
        Optional<ItemStack> item = service.cancel(player.getUniqueId(), listingId);
        if (item.isEmpty()) {
            player.sendMessage("§cEsa subasta ya no está disponible.");
            return;
        }
        player.sendMessage("§eRetiraste " + describe(item.get()) + " de la casa de subastas.");
        claim(player);
    }

    void claim(Player player) {
        if (service.hasPendingClaim(player.getUniqueId())) {
            player.sendMessage("§cTu entrega quedó pendiente de revisión. Contactá a un administrador.");
            return;
        }
        List<ItemStack> items = service.takeClaims(player.getUniqueId(), inventorySnapshot(player));
        if (items.isEmpty()) {
            player.sendMessage("§7No tenés items pendientes para reclamar.");
            return;
        }
        try {
            List<ItemStack> leftovers = new ArrayList<>();
            for (ItemStack item : items) {
                leftovers.addAll(player.getInventory().addItem(item.clone()).values());
            }
            service.recordClaimOutcome(player.getUniqueId(), inventorySnapshot(player), leftovers);
            String receipt = service.pendingClaim(player.getUniqueId()).orElseThrow().receiptId();
            PlayerDataCheckpoint.save(player, receiptKey, receipt);
            service.finishClaim(player.getUniqueId());
            int delivered = items.size() - leftovers.size();
            player.sendMessage(leftovers.isEmpty()
                ? "§aReclamaste " + delivered + " item(s)."
                : "§eReclamaste " + delivered + " item(s); " + leftovers.size() + " no entraron y siguen pendientes.");
        } catch (RuntimeException | java.io.IOException failure) {
            player.sendMessage("§cTu entrega quedó pendiente de revisión. Contactá a un administrador.");
            server.getLogger().log(java.util.logging.Level.SEVERE,
                "[AuctionHouse] Item handoff needs review for " + player.getUniqueId(), failure);
        }
    }

    void recoverClaim(Player player) {
        service.pendingClaim(player.getUniqueId()).ifPresent(receipt -> {
            try {
                if (receipt.inventoryAfter() != null && PlayerDataCheckpoint.contains(player, receiptKey, receipt.receiptId())) {
                    PlayerDataCheckpoint.confirm(player, receiptKey, receipt.receiptId());
                    service.finishClaim(player.getUniqueId());
                    player.sendMessage("§aSe confirmó tu entrega pendiente de la casa de subastas.");
                } else {
                    player.sendMessage("§cTenés una entrega pendiente de revisión. Contactá a un administrador.");
                }
            } catch (java.io.IOException | RuntimeException failure) {
                server.getLogger().log(java.util.logging.Level.SEVERE,
                    "[AuctionHouse] Could not verify pending claim for " + player.getUniqueId(), failure);
                player.sendMessage("§cTenés una entrega pendiente de revisión. Contactá a un administrador.");
            }
        });
    }

    void recoverListing(Player player) {
        service.pendingListing(player.getUniqueId()).ifPresent(pending -> {
            AuctionListingHold hold = pending.getValue();
            try {
                if (hold.inventoryAfter() != null && PlayerDataCheckpoint.contains(player, listingReceiptKey, hold.receiptId())) {
                    PlayerDataCheckpoint.confirm(player, listingReceiptKey, hold.receiptId());
                    service.finishListing(pending.getKey());
                    player.sendMessage("§aSe confirmó tu publicación pendiente en la casa de subastas.");
                } else {
                    player.sendMessage("§cTenés una publicación pendiente de revisión. Contactá a un administrador.");
                }
            } catch (java.io.IOException | RuntimeException failure) {
                server.getLogger().log(java.util.logging.Level.SEVERE,
                    "[AuctionHouse] Could not verify listing deposit for " + player.getUniqueId(), failure);
                player.sendMessage("§cTenés una publicación pendiente de revisión. Contactá a un administrador.");
            }
        });
    }

    private static String inventorySnapshot(Player player) {
        ItemStack[] items = player.getInventory().getContents().clone();
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] == null) items[slot] = ItemStack.empty();
        }
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(items));
    }

    static String describe(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().getKey().getKey().replace('_', ' ');
    }
}
