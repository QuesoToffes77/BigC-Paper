package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ChatFallbackShopUi implements ShopUiGateway {
    private final Server server;

    public ChatFallbackShopUi(Server server) {
        this.server = server;
    }

    @Override
    public void openShop(UUID playerId, ShopView shop) {
        Player player = server.getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.sendMessage("§6§l" + shop.title() + " §8| §fSaldo: " + shop.balance());
        player.sendMessage("§7" + shop.description());
        for (ShopViewItem item : shop.items()) {
            player.sendMessage((item.available() ? "§a" : "§c") + item.name() + " §8- §f" + item.price());
        }
    }
}
