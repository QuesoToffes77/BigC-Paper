package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CopperAppleConsumeListenerTest {

    @Test
    void effectsAreCommittedOnlyAfterOtherPluginsCanCancelConsumption() throws Exception {
        EventHandler handler = CopperAppleConsumeListener.class
            .getMethod("onPlayerConsume", PlayerItemConsumeEvent.class).getAnnotation(EventHandler.class);
        assertEquals(EventPriority.MONITOR, handler.priority());
        assertTrue(handler.ignoreCancelled());
    }

    @Test
    void cancelledConsumptionGrantsNoEffectsAndDoesNotUseCooldown() {
        AtomicInteger uses = new AtomicInteger();
        CopperAppleConsumeListener listener = listener(uses, new AtomicLong(0));
        Player player = player(GameMode.SURVIVAL);
        PlayerItemConsumeEvent cancelled = event(player, Material.APPLE);
        listener.onValidateConsume(cancelled);
        cancelled.setCancelled(true); // A protection plugin runs at HIGHEST.
        listener.onPlayerConsume(cancelled);
        assertEquals(0, uses.get());
        PlayerItemConsumeEvent next = event(player, Material.APPLE);
        listener.onValidateConsume(next);
        assertFalse(next.isCancelled());
        listener.onPlayerConsume(next);
        assertEquals(1, uses.get());
    }

    @Test
    void successfulConsumptionAppliesOnceAndCooldownExpires() {
        AtomicInteger uses = new AtomicInteger();
        AtomicLong clock = new AtomicLong(0);
        CopperAppleConsumeListener listener = listener(uses, clock);
        Player player = player(GameMode.SURVIVAL);
        PlayerItemConsumeEvent first = event(player, Material.APPLE);
        listener.onValidateConsume(first);
        listener.onPlayerConsume(first);
        assertEquals(1, uses.get());
        PlayerItemConsumeEvent blocked = event(player, Material.APPLE);
        listener.onValidateConsume(blocked);
        listener.onPlayerConsume(blocked);
        assertTrue(blocked.isCancelled());
        assertEquals(1, uses.get());
        clock.set(5000);
        PlayerItemConsumeEvent ready = event(player, Material.APPLE);
        listener.onValidateConsume(ready);
        assertFalse(ready.isCancelled());
        listener.onPlayerConsume(ready);
        assertEquals(2, uses.get());
    }

    @Test
    void spectatorAndOtherFoodsHaveNoCustomEffects() {
        AtomicInteger uses = new AtomicInteger();
        CopperAppleConsumeListener listener = listener(uses, new AtomicLong(0));
        PlayerItemConsumeEvent spectator = event(player(GameMode.SPECTATOR), Material.APPLE);
        listener.onValidateConsume(spectator);
        listener.onPlayerConsume(spectator);
        assertTrue(spectator.isCancelled());
        PlayerItemConsumeEvent vanilla = event(player(GameMode.SURVIVAL), Material.BREAD);
        listener.onValidateConsume(vanilla);
        listener.onPlayerConsume(vanilla);
        assertFalse(vanilla.isCancelled());
        assertEquals(0, uses.get());
    }

    @Test
    void logoutClearsCooldownState() {
        AtomicInteger uses = new AtomicInteger();
        CopperAppleConsumeListener listener = listener(uses, new AtomicLong(0));
        Player player = player(GameMode.SURVIVAL);
        listener.onPlayerConsume(event(player, Material.APPLE));
        listener.onPlayerQuit(new PlayerQuitEvent(player, "quit"));
        PlayerItemConsumeEvent next = event(player, Material.APPLE);
        listener.onValidateConsume(next);
        assertFalse(next.isCancelled());
    }

    private static CopperAppleConsumeListener listener(AtomicInteger uses, AtomicLong clock) {
        return new CopperAppleConsumeListener(item -> item.getType() == Material.APPLE,
            (player, item) -> uses.incrementAndGet(), 5000, clock::get);
    }

    private static PlayerItemConsumeEvent event(Player player, Material material) {
        return new PlayerItemConsumeEvent(player, new ItemStack(material), EquipmentSlot.HAND);
    }

    private static Player player(GameMode gameMode) {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getGameMode" -> gameMode;
                case "sendMessage" -> null;
                default -> throw new AssertionError("Unexpected player call: " + method.getName());
            });
    }
}
