package dev.linqfy.bigCasares.module.runtime;

import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;
import java.util.UUID;

/** A module temporarily owns or must recover a player's inventory. */
@FunctionalInterface
public interface InventoryCustody {
    boolean ownsInventory(UUID playerId);

    /** Checks every other registered owner; an unavailable provider fails closed. */
    static boolean ownedByOther(Server server, UUID playerId, InventoryCustody self) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        try {
            for (RegisteredServiceProvider<InventoryCustody> registration :
                server.getServicesManager().getRegistrations(InventoryCustody.class)) {
                InventoryCustody provider = registration.getProvider();
                if (provider != self && provider.ownsInventory(playerId)) return true;
            }
            return false;
        } catch (RuntimeException failure) {
            return true;
        }
    }
}
