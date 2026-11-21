package dev.linqfy.bigCasares.modules.netheritequest;

import dev.linqfy.bigCasares.module.runtime.InventoryCustody;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class NetheriteQuestCustodyTest {
    @Test
    void foreignCustodyDefersActivationAndPendingRewardsWithoutTouchingInventory(@TempDir Path dir) {
        UUID id = UUID.randomUUID();
        AtomicBoolean unavailable = new AtomicBoolean();
        InventoryCustody foreign = playerId -> {
            if (unavailable.get()) throw new IllegalStateException("provider unavailable");
            return playerId.equals(id);
        };
        var registration = new RegisteredServiceProvider<>(InventoryCustody.class, foreign, ServicePriority.Normal, null);
        ServicesManager services = (ServicesManager) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{ServicesManager.class}, (proxy, method, args) ->
                method.getName().equals("getRegistrations") ? List.of(registration) : null);
        Server server = (Server) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Server.class},
            (proxy, method, args) -> method.getName().equals("getServicesManager") ? services : null);
        Player player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getUniqueId")) return id;
                if (method.getName().equals("getInventory")) throw new AssertionError("inventory touched during custody");
                return null;
            });
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.addPendingSoul(id, "boss", "Boss", 1);
        NetheriteQuestRuntime runtime = new NetheriteQuestRuntime(server, Logger.getAnonymousLogger(), book,
            new NetheriteQuestStore(dir.resolve("quests.yml")), null, () -> 0L,
            NamespacedKey.minecraft("test_armor"), NamespacedKey.minecraft("test_activation"), NamespacedKey.minecraft("test_soul"));
        assertDoesNotThrow(() -> runtime.activate(player, ArmorQuestPiece.HELMET));
        unavailable.set(true);
        assertDoesNotThrow(() -> runtime.claimRewards(player));
        assertFalse(book.hasActive(id, ArmorQuestPiece.HELMET));
        assertEquals(1, book.view(id).pendingSouls().get("boss").amount());
        assertTrue(book.soulDelivery(id).isEmpty());
        book.activate(id, ArmorQuestPiece.HELMET, List.of("boss"), 0, 0L);
        book.beginActivationReceipt(id, ArmorQuestPiece.HELMET, "boss", 0, "before", 0, 0f, 0);
        String receipt = book.activationReceipt(id).orElseThrow().receiptId();
        assertFalse(runtime.resolveReceipt(id, receipt, "activacion", false));
        assertTrue(book.activationPending(id));
    }
}
