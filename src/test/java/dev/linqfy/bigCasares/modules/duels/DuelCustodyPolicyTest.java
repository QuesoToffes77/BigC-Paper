package dev.linqfy.bigCasares.modules.duels;

import dev.linqfy.bigCasares.module.runtime.InventoryCustody;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DuelCustodyPolicyTest {
    @Test void reservesSessionsAndPendingOrUnverifiedSnapshots() {
        assertTrue(DuelCustodyPolicy.owns(true, false, false, false, false));
        assertTrue(DuelCustodyPolicy.owns(true, true, true, true, true),
            "an active session or checkpoint remains reserved even if its marker matches");
        assertTrue(DuelCustodyPolicy.owns(false, true, false, true, false));
        assertTrue(DuelCustodyPolicy.owns(false, false, true, false, false));
        assertTrue(DuelCustodyPolicy.owns(false, false, true, true, false));
        assertFalse(DuelCustodyPolicy.owns(false, false, true, true, true));
        assertFalse(DuelCustodyPolicy.owns(false, true, true, true, true));
        assertFalse(DuelCustodyPolicy.owns(false, false, false, true, false));
    }

    @Test void excludesSelfAndFailsClosedOnAnotherOwnerOrProviderError() {
        UUID player = UUID.randomUUID();
        InventoryCustody self = id -> true;
        InventoryCustody other = id -> id.equals(player);
        InventoryCustody failing = id -> { throw new IllegalStateException("unavailable"); };
        assertFalse(InventoryCustody.ownedByOther(server(self), player, self));
        assertTrue(InventoryCustody.ownedByOther(server(self, other), player, self));
        assertTrue(InventoryCustody.ownedByOther(server(self, failing), player, self));
    }

    private static Server server(InventoryCustody... providers) {
        var registrations = java.util.Arrays.stream(providers)
            .map(provider -> new RegisteredServiceProvider<>(InventoryCustody.class, provider, ServicePriority.Normal, null))
            .toList();
        ServicesManager services = (ServicesManager) Proxy.newProxyInstance(ServicesManager.class.getClassLoader(),
            new Class<?>[]{ServicesManager.class}, (object, method, args) ->
                method.getName().equals("getRegistrations") ? registrations : null);
        return (Server) Proxy.newProxyInstance(Server.class.getClassLoader(), new Class<?>[]{Server.class},
            (object, method, args) -> method.getName().equals("getServicesManager") ? services : null);
    }
}
