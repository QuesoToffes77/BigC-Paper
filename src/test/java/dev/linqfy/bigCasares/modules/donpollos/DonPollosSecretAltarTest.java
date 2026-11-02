package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El menu del secreto contra un inventario de jugador falso (las tandas de stacks, devolver, que no alcance). */
class DonPollosSecretAltarTest {

    /** En el test cada cosa que pide el Gordito es un material distinto (en el juego son items de Don Pollos). */
    private static final Map<DonPollosSecretOffering, Material> MATERIALS = Map.of(
        DonPollosSecretOffering.SALSA, Material.PAPER,
        DonPollosSecretOffering.PICANTE, Material.STICK,
        DonPollosSecretOffering.POLLO_FRITO, Material.COOKED_CHICKEN,
        DonPollosSecretOffering.DIAMANTES, Material.DIAMOND);

    private final DonPollosSecretAltar altar = new DonPollosSecretAltar(new DonPollosSecretAltar.Goods() {
        @Override
        public boolean matches(DonPollosSecretOffering offering, ItemStack stack) {
            return stack != null && stack.getType() == MATERIALS.get(offering);
        }

        @Override
        public ItemStack stack(DonPollosSecretOffering offering) {
            return new ItemStack(MATERIALS.get(offering), offering.amount);
        }
    });

    private final ItemStack[] slots = new ItemStack[36];
    private final List<ItemStack> dropped = new ArrayList<>();
    private final PlayerInventory inventory = fakeInventory(slots);
    private final Set<DonPollosSecretOffering> placed = EnumSet.noneOf(DonPollosSecretOffering.class);

    private static PlayerInventory fakeInventory(ItemStack[] slots) {
        return (PlayerInventory) Proxy.newProxyInstance(DonPollosSecretAltarTest.class.getClassLoader(),
            new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getStorageContents" -> slots.clone();
                case "setItem" -> {
                    slots[(Integer) args[0]] = (ItemStack) args[1];
                    yield null;
                }
                case "addItem" -> {
                    HashMap<Integer, ItemStack> leftovers = new HashMap<>();
                    ItemStack[] stacks = (ItemStack[]) args[0];
                    for (int i = 0; i < stacks.length; i++) {
                        int free = java.util.Arrays.asList(slots).indexOf(null);
                        if (free < 0) {
                            leftovers.put(i, stacks[i]);
                        } else {
                            slots[free] = stacks[i];
                        }
                    }
                    yield leftovers;
                }
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakePlayerInventory";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private int total(Material material) {
        int amount = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.getType() == material) {
                amount += stack.getAmount();
            }
        }
        return amount;
    }

    @Test
    void aStackOfChickenSplitAcrossSlotsIsTakenExactly() {
        slots[0] = new ItemStack(Material.COOKED_CHICKEN, 30);
        slots[5] = new ItemStack(Material.COOKED_CHICKEN, 30);
        slots[9] = new ItemStack(Material.COOKED_CHICKEN, 10);

        assertEquals(DonPollosSecretAltar.Result.PLACED,
            altar.toggle(inventory, placed, DonPollosSecretOffering.POLLO_FRITO, dropped::add));

        assertEquals(6, total(Material.COOKED_CHICKEN), "70 - 64");
        assertTrue(placed.contains(DonPollosSecretOffering.POLLO_FRITO));
    }

    @Test
    void notEnoughTakesNothing() {
        slots[0] = new ItemStack(Material.DIAMOND, 3);

        assertEquals(DonPollosSecretAltar.Result.MISSING,
            altar.toggle(inventory, placed, DonPollosSecretOffering.DIAMANTES, dropped::add));

        assertEquals(3, total(Material.DIAMOND));
        assertEquals(3, altar.count(inventory, DonPollosSecretOffering.DIAMANTES));
        assertFalse(placed.contains(DonPollosSecretOffering.DIAMANTES));
    }

    @Test
    void clickingAgainGivesItBack() {
        slots[0] = new ItemStack(Material.DIAMOND, 5);
        altar.toggle(inventory, placed, DonPollosSecretOffering.DIAMANTES, dropped::add);
        assertEquals(0, total(Material.DIAMOND));

        assertEquals(DonPollosSecretAltar.Result.RETURNED,
            altar.toggle(inventory, placed, DonPollosSecretOffering.DIAMANTES, dropped::add));

        assertEquals(5, total(Material.DIAMOND));
        assertTrue(placed.isEmpty());
    }

    @Test
    void closingTheMenuReturnsEverythingPlaced() {
        slots[0] = new ItemStack(Material.PAPER, 1);
        slots[1] = new ItemStack(Material.STICK, 2);
        altar.toggle(inventory, placed, DonPollosSecretOffering.SALSA, dropped::add);
        altar.toggle(inventory, placed, DonPollosSecretOffering.PICANTE, dropped::add);

        altar.returnAll(inventory, placed, dropped::add);

        assertEquals(1, total(Material.PAPER));
        assertEquals(2, total(Material.STICK));
        assertTrue(placed.isEmpty());
        assertTrue(dropped.isEmpty());
    }

    @Test
    void whatDoesNotFitIsDropped() {
        slots[0] = new ItemStack(Material.DIAMOND, 5);
        altar.toggle(inventory, placed, DonPollosSecretOffering.DIAMANTES, dropped::add);
        java.util.Arrays.fill(slots, new ItemStack(Material.DIRT, 64));

        altar.returnAll(inventory, placed, dropped::add);

        assertEquals(1, dropped.size());
        assertEquals(Material.DIAMOND, dropped.getFirst().getType());
        assertEquals(5, dropped.getFirst().getAmount());
    }
}
