package dev.linqfy.bigCasares.modules.auth;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.runtime.InventoryCustody;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

/** Whether inventory-delivery tasks may operate on the player's real inventory. */
public final class AuthReadiness {
    private AuthReadiness() {
    }

    public static boolean ready(JavaPlugin plugin, Player player) {
        boolean pending = false;
        if (plugin instanceof BigCasares bigCasares) {
            AuthModule auth = bigCasares.getAuthModule();
            pending = auth != null && auth.isPending(player);
        }
        return ready(plugin.getServer(), player.getUniqueId(), pending);
    }

    static boolean ready(Server server, UUID playerId, boolean authPending) {
        return !authPending && !InventoryCustody.ownedByOther(server, playerId, null);
    }

}
