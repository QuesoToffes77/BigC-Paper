package dev.linqfy.bigCasares.modules.donpollos;

import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Server;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosModuleWiringTest {

    @Test
    void moduleIdIsStable() {
        assertEquals("don-pollos", new DonPollosModule(null).getId());
        assertEquals("don-pollos", DonPollosModule.MODULE_ID);
    }

    @Test
    void enablingWithoutAPluginIsANoOp() {
        DonPollosModule module = new DonPollosModule(null);
        module.onEnable();
        module.onDisable();
    }

    @Test
    void menusAreClosedBeforeTheirListenersAreRemoved() {
        // Las limpiezas de BigC corren en orden inverso al registro. Para que los menus se cierren mientras sus
        // listeners todavia protegen los clicks y devuelven apuestas e ingredientes, "don-pollos-close-menus"
        // tiene que registrarse despues de todos los listeners. Se prueba con la secuencia real del modulo.
        List<Listener> all = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            all.add(new ProbeListener());
        }
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(fakePlugin(), scope);
        List<Boolean> registeredWhenMenusClosed = new ArrayList<>();
        List<String> events = new ArrayList<>();

        DonPollosModule.registerListenersThenMenuCleanup(registrations,
            new DonPollosModule.ModuleListeners(all.get(0), all.get(1), all.get(2), all.get(3), all.get(4),
                all.get(5), all.get(6)), () -> {
                events.add("menus-closed");
                for (Listener listener : all) {
                    registeredWhenMenusClosed.add(isRegistered(listener));
                }
            });
        assertTrue(all.stream().allMatch(DonPollosModuleWiringTest::isRegistered));

        scope.close();

        assertEquals(List.of("menus-closed"), events);
        assertEquals(List.of(true, true, true, true, true, true, true), registeredWhenMenusClosed);
        assertTrue(all.stream().noneMatch(DonPollosModuleWiringTest::isRegistered));
    }

    @Test
    void disabledCitiesAreSkippedButMenusStillCloseFirst() {
        Listener[] listeners = new Listener[6];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = new ProbeListener();
        }
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(fakePlugin(), scope);
        List<Boolean> stillRegistered = new ArrayList<>();

        DonPollosModule.registerListenersThenMenuCleanup(registrations,
            new DonPollosModule.ModuleListeners(listeners[0], listeners[1], listeners[2], listeners[3], null,
                listeners[4], listeners[5]), () -> {
                for (Listener listener : listeners) {
                    stillRegistered.add(isRegistered(listener));
                }
            });
        scope.close();

        assertEquals(List.of(true, true, true, true, true, true), stillRegistered);
        for (Listener listener : listeners) {
            assertFalse(isRegistered(listener));
        }
    }

    /** Un listener observable: queda en la HandlerList mientras este registrado. */
    private static final class ProbeListener implements Listener {
    }

    private static boolean isRegistered(Listener listener) {
        return Arrays.stream(InventoryCloseEvent.getHandlerList().getRegisteredListeners())
            .anyMatch(registered -> registered.getListener() == listener);
    }

    private static Plugin fakePlugin() {
        ClassLoader loader = DonPollosModuleWiringTest.class.getClassLoader();
        PluginManager manager = (PluginManager) Proxy.newProxyInstance(loader, new Class<?>[]{PluginManager.class},
            (proxy, method, args) -> {
                if (method.getName().equals("registerEvents")) {
                    InventoryCloseEvent.getHandlerList().register(new RegisteredListener((Listener) args[0],
                        (listener, event) -> { }, EventPriority.NORMAL, (Plugin) args[1], false));
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        Server server = (Server) Proxy.newProxyInstance(loader, new Class<?>[]{Server.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getPluginManager")) {
                    return manager;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        return (Plugin) Proxy.newProxyInstance(loader, new Class<?>[]{Plugin.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getServer" -> server;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakePlugin";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
