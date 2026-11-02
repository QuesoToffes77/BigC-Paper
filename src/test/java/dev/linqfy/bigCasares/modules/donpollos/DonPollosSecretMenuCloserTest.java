package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cerrar el menu del secreto con un InventoryCloseEvent real: se devuelve lo puesto salvo que ya se haya usado. */
class DonPollosSecretMenuCloserTest {

    private final DonPollosSecretMenuCloser closer = new DonPollosSecretMenuCloser(
        new DonPollosSecretAltar(new DonPollosSecretAltar.Goods() {
            @Override
            public boolean matches(DonPollosSecretOffering offering, ItemStack stack) {
                return false;
            }

            @Override
            public ItemStack stack(DonPollosSecretOffering offering) {
                return new ItemStack(Material.PAPER, offering.amount);
            }
        }));
    private final List<ItemStack> given = new ArrayList<>();
    private final List<ItemStack> dropped = new ArrayList<>();
    private final List<Player> closedFor = new ArrayList<>();
    private final Player player = fakePlayer();

    private static final class FakeSession implements DonPollosSecretMenuCloser.Session {
        private final Set<DonPollosSecretOffering> placed = EnumSet.noneOf(DonPollosSecretOffering.class);
        private final boolean done;

        private FakeSession(boolean done) {
            this.done = done;
        }

        @Override
        public boolean done() {
            return done;
        }

        @Override
        public Set<DonPollosSecretOffering> placed() {
            return placed;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    /** Inventario falso donde solo entran stacks de hasta 10 (el resto sale como sobrante). */
    private Player fakePlayer() {
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
                if (method.getName().equals("addItem")) {
                    HashMap<Integer, ItemStack> leftovers = new HashMap<>();
                    for (ItemStack stack : (ItemStack[]) args[0]) {
                        if (stack.getAmount() > 10) {
                            leftovers.put(leftovers.size(), stack);
                        } else {
                            given.add(stack);
                        }
                    }
                    return leftovers;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getInventory" -> inventory;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private InventoryCloseEvent closeEvent(InventoryHolder holder) {
        Inventory top = (Inventory) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{Inventory.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getHolder" -> holder;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
        InventoryView view = (InventoryView) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{InventoryView.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getTopInventory" -> top;
                case "getPlayer" -> player;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
        return new InventoryCloseEvent(view);
    }

    @Test
    void closingBeforeFinishingReturnsEverythingPlaced() {
        FakeSession session = new FakeSession(false);
        session.placed.add(DonPollosSecretOffering.SALSA);
        session.placed.add(DonPollosSecretOffering.DIAMANTES);

        closer.handle(closeEvent(session), closedFor::add, (target, stack) -> dropped.add(stack));

        assertEquals(List.of(player), closedFor);
        assertEquals(2, given.size());
        assertTrue(dropped.isEmpty());
        assertEquals(DonPollosSecretOffering.SALSA.amount + DonPollosSecretOffering.DIAMANTES.amount,
            given.stream().mapToInt(ItemStack::getAmount).sum());
        assertTrue(session.placed.isEmpty());
    }

    @Test
    void whatDoesNotFitIsDropped() {
        FakeSession session = new FakeSession(false);
        session.placed.add(DonPollosSecretOffering.POLLO_FRITO);  // 64: no entra en el inventario falso

        closer.handle(closeEvent(session), closedFor::add, (target, stack) -> dropped.add(stack));

        assertEquals(1, dropped.size());
        assertTrue(given.isEmpty());
    }

    @Test
    void closingAfterFinishingReturnsNothingButStillNotifies() {
        FakeSession session = new FakeSession(true);
        session.placed.add(DonPollosSecretOffering.SALSA);

        closer.handle(closeEvent(session), closedFor::add, (target, stack) -> dropped.add(stack));

        assertEquals(List.of(player), closedFor);
        assertTrue(given.isEmpty());
        assertTrue(dropped.isEmpty());
        assertEquals(1, session.placed.size());
    }

    @Test
    void otherInventoriesAreIgnored() {
        closer.handle(closeEvent(() -> null), closedFor::add, (target, stack) -> dropped.add(stack));

        assertTrue(closedFor.isEmpty());
        assertTrue(given.isEmpty());
    }
}
