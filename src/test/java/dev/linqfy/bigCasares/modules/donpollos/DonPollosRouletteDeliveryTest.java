package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La entrega de la ruleta (pago, liquidacion y devolucion usan la misma) contra un jugador falso. */
class DonPollosRouletteDeliveryTest {

    private final List<ItemStack> inserted = new ArrayList<>();
    private final List<ItemStack> dropped = new ArrayList<>();
    private final List<String> order = new ArrayList<>();
    private int freeSlots = 100;
    private final Player player = fakePlayer();

    private Player fakePlayer() {
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
                if (method.getName().equals("addItem")) {
                    HashMap<Integer, ItemStack> leftovers = new HashMap<>();
                    ItemStack[] stacks = (ItemStack[]) args[0];
                    for (int i = 0; i < stacks.length; i++) {
                        if (freeSlots-- > 0) {
                            inserted.add(stacks[i]);
                            order.add("insert");
                        } else {
                            leftovers.put(i, stacks[i]);
                        }
                    }
                    return leftovers;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{World.class},
            (proxy, method, args) -> {
                if (method.getName().equals("dropItemNaturally")) {
                    dropped.add((ItemStack) args[1]);
                    order.add("drop");
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            });
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getInventory" -> inventory;
                case "getWorld" -> world;
                case "getLocation" -> null;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    /** getMaxStackSize consulta el registro del servidor (no hay uno en los tests), asi que se fija a mano en 64. */
    private static ItemStack stackOf(Material material) {
        return new ItemStack(material) {
            @Override
            public int getMaxStackSize() {
                return 64;
            }
        };
    }

    @Test
    void payoutIsSplitInStacksAndTheLimiterRunsAfterTheDelivery() {
        List<Player> limited = new ArrayList<>();

        DonPollosRouletteDelivery.give(player, stackOf(Material.DIAMOND), 130, target -> {
            order.add("limit");
            limited.add(target);
        });

        assertEquals(3, inserted.size());
        assertEquals(130, inserted.stream().mapToInt(ItemStack::getAmount).sum());
        assertEquals(List.of(player), limited);
        assertEquals("limit", order.getLast());
    }

    @Test
    void inactiveLimiterStillDeliversEverything() {
        DonPollosRouletteDelivery.give(player, stackOf(Material.GOLD_INGOT), 5, target -> { });

        assertEquals(1, inserted.size());
        assertEquals(5, inserted.get(0).getAmount());
        assertEquals(Material.GOLD_INGOT, inserted.get(0).getType());
    }

    @Test
    void whatDoesNotFitIsDroppedBeforeTheLimiterRuns() {
        freeSlots = 1;

        DonPollosRouletteDelivery.give(player, stackOf(Material.DIAMOND), 128, target -> order.add("limit"));

        assertEquals(1, inserted.size());
        assertEquals(1, dropped.size());
        assertEquals(List.of("insert", "drop", "limit"), order);
    }
}
