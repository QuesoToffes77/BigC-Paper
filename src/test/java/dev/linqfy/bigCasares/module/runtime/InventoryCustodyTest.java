package dev.linqfy.bigCasares.module.runtime;

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

class InventoryCustodyTest {
    @Test
    void ignoresOnlyTheCallingProviderAndChecksAllOthers() {
        UUID player = UUID.randomUUID();
        InventoryCustody self = id -> true;
        InventoryCustody idle = id -> false;
        InventoryCustody owner = player::equals;
        assertFalse(InventoryCustody.ownedByOther(server(List.of(self, idle)), player, self));
        assertTrue(InventoryCustody.ownedByOther(server(List.of(self, idle, owner)), player, self));
        assertTrue(InventoryCustody.ownedByOther(server(List.of(self)), player, null));
    }

    @Test
    void aFailingProviderIsTreatedAsHoldingTheInventory() {
        InventoryCustody broken = id -> { throw new IllegalStateException("unavailable"); };
        assertTrue(InventoryCustody.ownedByOther(server(List.of(broken)), UUID.randomUUID(), null));
    }

    private static Server server(List<InventoryCustody> providers) {
        var registrations = providers.stream().map(provider -> new RegisteredServiceProvider<>(
            InventoryCustody.class, provider, ServicePriority.Normal, null)).toList();
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
