package dev.linqfy.bigCasares.modules.auth;

import dev.linqfy.bigCasares.module.runtime.InventoryCustody;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthReadinessTest {
    @Test
    void requiresBothAuthenticationAndEveryInventoryOwnerToRelease() {
        UUID player = UUID.randomUUID();
        Server idle = server(List.of(id -> false));
        Server owned = server(List.of(player::equals));
        assertFalse(AuthReadiness.ready(idle, player, true));
        assertFalse(AuthReadiness.ready(owned, player, false));
        assertTrue(AuthReadiness.ready(idle, player, false));
    }

    @Test
    void failingInventoryOwnerBlocksDelivery() {
        InventoryCustody broken = id -> { throw new IllegalStateException("unavailable"); };
        assertFalse(AuthReadiness.ready(server(List.of(broken)), UUID.randomUUID(), false));
    }

    private static Server server(List<InventoryCustody> owners) {
        var registrations = owners.stream().map(owner -> new RegisteredServiceProvider<>(
            InventoryCustody.class, owner, ServicePriority.Normal, null)).toList();
        ServicesManager services = (ServicesManager) Proxy.newProxyInstance(ServicesManager.class.getClassLoader(),
            new Class<?>[]{ServicesManager.class}, (proxy, method, args) -> {
                if (method.getName().equals("getRegistrations")) return registrations;
                throw new UnsupportedOperationException(method.getName());
            });
        return (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getServicesManager")) return services;
                throw new UnsupportedOperationException(method.getName());
            });
    }
}
