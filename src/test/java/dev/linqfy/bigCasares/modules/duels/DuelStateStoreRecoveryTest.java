package dev.linqfy.bigCasares.modules.duels;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class DuelStateStoreRecoveryTest {
    @TempDir Path directory;

    @Test void anotherOwnerDefersRecoveryBeforeTouchingInventoryOrSnapshot() throws Exception {
        UUID id = UUID.randomUUID();
        var file = directory.resolve(id + ".yml");
        var yaml = new YamlConfiguration();
        yaml.set("revision", UUID.randomUUID().toString());
        yaml.save(file.toFile());
        var inventoryReads = new AtomicInteger();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (target, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getName" -> "player";
                case "getInventory" -> { inventoryReads.incrementAndGet(); yield null; }
                default -> null;
            });
        var store = new DuelStateStore(directory.toFile(), null, Logger.getAnonymousLogger());
        assertEquals(-2L, store.restore(player, () -> false));
        assertEquals(0, inventoryReads.get());
        assertFalse(YamlConfiguration.loadConfiguration(file.toFile()).getBoolean("recovery-pending"));
    }

    @Test void ownershipAcquiredByCloseCallbackDefersRecoveryBeforeChangingSnapshot() throws Exception {
        UUID id = UUID.randomUUID();
        var file = directory.resolve(id + ".yml");
        var yaml = new YamlConfiguration();
        yaml.set("revision", UUID.randomUUID().toString());
        yaml.save(file.toFile());
        var closes = new AtomicInteger();
        var checks = new AtomicInteger();
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
            new Class<?>[]{PlayerInventory.class}, (target, method, args) ->
                method.getName().equals("getContents") ? new ItemStack[41] : null);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (target, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getName" -> "player";
                case "getInventory" -> inventory;
                case "closeInventory" -> { closes.incrementAndGet(); yield null; }
                default -> null;
            });
        var store = new DuelStateStore(directory.toFile(), null, Logger.getAnonymousLogger());
        assertEquals(-2L, store.restore(player, () -> checks.getAndIncrement() == 0));
        assertEquals(1, closes.get());
        assertEquals(2, checks.get());
        assertFalse(YamlConfiguration.loadConfiguration(file.toFile()).getBoolean("recovery-pending"));
    }

    @Test void ownershipAcquiredByCloseCallbackPreventsSnapshot() {
        UUID id = UUID.randomUUID();
        var closes = new AtomicInteger();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (target, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "closeInventory" -> { closes.incrementAndGet(); yield null; }
                default -> null;
            });
        var store = new DuelStateStore(directory.toFile(), null, Logger.getAnonymousLogger());
        assertFalse(store.save(player, 0, () -> false));
        assertEquals(1, closes.get());
        assertFalse(store.has(id));
    }

    @Test void uncertainRecoveryRemainsOnDiskWithoutTouchingInventory() throws Exception {
        UUID id = UUID.randomUUID();
        var file = directory.resolve(id + ".yml");
        var yaml = new YamlConfiguration();
        yaml.set("revision", UUID.randomUUID().toString());
        yaml.set("recovery-pending", true);
        yaml.save(file.toFile());
        var inventoryReads = new AtomicInteger();
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (target, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getName" -> "player";
                case "getInventory" -> { inventoryReads.incrementAndGet(); yield null; }
                default -> null;
            });
        var store = new DuelStateStore(directory.toFile(), null, Logger.getAnonymousLogger());
        assertEquals(-1L, store.restore(player));
        assertEquals(0, inventoryReads.get());
        assertTrue(file.toFile().isFile());
        assertTrue(YamlConfiguration.loadConfiguration(file.toFile()).getBoolean("recovery-pending"));
        assertEquals(-1L, store.restore(player));
        assertEquals(0, inventoryReads.get(), "an uncertain recovery must never be applied again");
    }
}
