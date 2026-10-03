package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.command.BigCasaresCommand;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import dev.linqfy.bigCasares.items.catalog.CustomItemCatalog;
import dev.linqfy.bigCasares.items.catalog.ItemCatalogLoader;
import dev.linqfy.bigCasares.items.catalog.ItemCatalogSeeder;
import dev.linqfy.bigCasares.modules.items.ItemCatalogModule;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests of the real {@code /bigcasares give} delivery path for the
 * six Grappling Hook tiers. The setup mirrors {@code ItemCatalogModule.onEnable}
 * exactly: the packaged defaults are seeded into a data folder, the loader
 * reads that folder, the catalog is installed, and each tier is registered as
 * a {@link GrapplingHookItem}. Before {@code DEFAULT_FILES} was fixed, the
 * seeded catalog had no {@code grappling_hook_*} definitions, so
 * {@code createItemStack} threw "unknown custom item id" and Bukkit reported
 * "An unexpected error occurred whilst trying to execute that command".
 */
class GrapplingHookGiveCommandTest {

    @TempDir
    Path tempDir;

    private BigCasares plugin;
    private BigCasaresCommand command;
    private Command bigcasaresCommand;
    private FakePlayer self;
    private FakePlayer other;

    @BeforeEach
    void setUp() throws Exception {
        plugin = allocatePlugin();
        CustomItemRegistry registry = new CustomItemRegistry();

        Path items = tempDir.resolve("items");
        List<String> defaultFiles = defaultFiles();
        new ItemCatalogSeeder(defaultFiles).seed(
            name -> getClass().getClassLoader().getResourceAsStream("content/items/" + name),
            items);
        CustomItemCatalog catalog = new ItemCatalogLoader().load(items);
        registry.installCatalog(catalog);
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            registry.register(new GrapplingHookItem(tier, registry, new NamespacedKey("bigcasares", tier.catalogId())));
        }

        Field registryField = BigCasares.class.getDeclaredField("customItemRegistry");
        registryField.setAccessible(true);
        registryField.set(plugin, registry);

        command = new BigCasaresCommand(plugin);
        bigcasaresCommand = new Command("bigcasares") {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return false;
            }
        };

        self = new FakePlayer("locobestiaparda");
        other = new FakePlayer("otrojugador");
        setBukkitServer(server(self, other));
    }

    @AfterEach
    void tearDown() throws Exception {
        setBukkitServer(null);
    }

    @Test
    void productionDefaultsSeedACatalogThatCanBuildEveryHookStack() throws Exception {
        // The exact failure the plugin hit on the real server: the seeded
        // catalog had no grappling_hook_* definitions, so the factory threw and
        // the command died with an unexpected error. Now every tier must both
        // resolve by id and produce a real ItemStack.
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            assertTrue(plugin.getCustomItemRegistry().findById(tier.catalogId()).isPresent(),
                tier.catalogId() + " must resolve after the defaults are seeded");
            assertDoesNotThrow(() -> plugin.getCustomItemRegistry().createItemStack(tier.catalogId(), 1),
                tier.catalogId() + " stack must be creatable");
        }
    }

    @Test
    void independentlyCreatedNitricAcidStacksKeepTheSameIdentityAndConfiguredSize() {
        ItemStack first = plugin.getCustomItemRegistry().createItemStack("nitric_acid", 1);
        ItemStack second = plugin.getCustomItemRegistry().createItemStack("nitric_acid", 1);

        assertEquals(64, first.getItemMeta().getMaxStackSize());
        assertEquals(first.getType(), second.getType());
        assertEquals(first.getItemMeta().getMaxStackSize(), second.getItemMeta().getMaxStackSize());
        assertEquals("nitric_acid", itemId(first));
        assertEquals("nitric_acid", itemId(second));
    }

    @Test
    void giveDeliversEveryTierToTheSender() {
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            self.reset();

            boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
                new String[]{"give", tier.catalogId()});

            assertTrue(ok, tier.catalogId() + " must execute without an exception");
            assertTrue(self.messages.get(0).contains("Received 1x " + tier.catalogId() + "."),
                "expected delivery confirmation, got: " + self.messages);
            assertEquals(1, self.received.size());
            ItemStack stack = self.received.get(0);
            assertEquals(1, stack.getAmount());
            assertEquals(tier.catalogId(), itemId(stack));
        }
    }

    @Test
    void giveSupportsExplicitAmounts() {
        // potassium_nitrate is a catalog-only item with max-stack-size 64: the
        // requested amount must be honored end to end.
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "potassium_nitrate", "5"});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Received 5x potassium_nitrate."));
        assertEquals(1, self.received.size());
        assertEquals(5, self.received.get(0).getAmount());
    }

    @Test
    void hooksAreClampedToTheirMaxStackSize() {
        // Hooks are max-stack-size 1; the command echoes the requested amount
        // but the delivered stack is clamped, exactly like a vanilla /give of an
        // unstackable item.
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook_1", "10"});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Received 10x grappling_hook_1."));
        assertEquals(1, self.received.get(0).getAmount());
    }

    @Test
    void giveRejectsInvalidAmounts() {
        for (String amount : new String[]{"0", "-1", "65"}) {
            self.reset();

            boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
                new String[]{"give", "grappling_hook_1", amount});

            assertTrue(ok, "invalid amount must be handled, not thrown");
            assertTrue(self.messages.get(0).contains("Amount must be between 1 and 64."),
                "expected amount error for " + amount + ", got: " + self.messages);
            assertEquals(0, self.received.size());
        }
    }

    @Test
    void giveRejectsUnknownItemIdsWithoutThrowing() {
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "does_not_exist"});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Unknown item id: does_not_exist"));
        assertEquals(0, self.received.size());
    }

    @Test
    void giveReportsMissingItemId() {
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give"});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Missing item id."));
        assertEquals(0, self.received.size());
    }

    @Test
    void giveToSelfByExplicitNameDeliversTheItem() {
        // The exact command that crashed on the real server: a player giving a
        // hook to themselves by name. The item resolves, the player is found,
        // and the stack is delivered instead of throwing.
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook_1", self.name});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Received 1x grappling_hook_1."),
            "got: " + self.messages);
        assertEquals(1, self.received.size());
        assertEquals("grappling_hook_1", itemId(self.received.get(0)));
    }

    @Test
    void giveToAnotherPlayerDeliversTheItem() {
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook_2", other.name});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Gave 1x grappling_hook_2 to otrojugador."),
            "got: " + self.messages);
        assertEquals(0, self.received.size());
        assertEquals(1, other.received.size());
        assertEquals("grappling_hook_2", itemId(other.received.get(0)));
        assertTrue(other.messages.get(0).contains("Received 1x grappling_hook_2 from locobestiaparda."),
            "got: " + other.messages);
    }

    @Test
    void giveReportsAnUnknownPlayerWithoutThrowing() {
        boolean ok = command.onCommand(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook_1", "nobody"});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Player not found or offline: nobody"));
        assertEquals(0, self.received.size());
    }

    @Test
    void giveFromConsoleRequiresAndReachesAPlayerTarget() {
        CommandSender console = proxy(CommandSender.class, (method, args) -> {
            if ("sendMessage".equals(method.getName()) && args.length > 0 && args[0] instanceof String text) {
                self.messages.add(text);
                return null;
            }
            return defaultValue(method.getReturnType());
        });

        boolean ok = command.onCommand(console, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook_2", self.name});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Gave 1x grappling_hook_2 to locobestiaparda."),
            "got: " + self.messages);
        assertEquals(1, self.received.size());
        assertEquals("grappling_hook_2", itemId(self.received.get(0)));
    }
    @Test
    void giveFromConsoleWithoutTargetReportsUsage() {
        CommandSender console = proxy(CommandSender.class, (method, args) -> {
            if ("sendMessage".equals(method.getName()) && args.length > 0 && args[0] instanceof String text) {
                self.messages.add(text);
                return null;
            }
            return defaultValue(method.getReturnType());
        });

        boolean ok = command.onCommand(console, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook_1"});

        assertTrue(ok);
        assertTrue(self.messages.get(0).contains("Console usage requires a player target."));
    }

    @Test
    void tabCompletionListsAllSixTiers() {
        List<String> suggestions = command.onTabComplete(self.player, bigcasaresCommand, "bigcasares",
            new String[]{"give", "grappling_hook"});

        assertEquals(List.of(
            "grappling_hook_1",
            "grappling_hook_2",
            "grappling_hook_3",
            "grappling_hook_4",
            "grappling_hook_5",
            "grappling_hook_6"
        ), suggestions);
    }

    private String itemId(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        Object value = meta.getPersistentDataContainer().get(
            new NamespacedKey("bigcasares", "item_id"), PersistentDataType.STRING);
        return (String) value;
    }
    @SuppressWarnings("unchecked")
    private static List<String> defaultFiles() throws Exception {
        Field field = ItemCatalogModule.class.getDeclaredField("DEFAULT_FILES");
        field.setAccessible(true);
        return (List<String>) field.get(null);
    }

    private static BigCasares allocatePlugin() throws Exception {
        // JavaPlugin's constructor requires a PluginClassLoader, so the plugin
        // is allocated without running it (fields are then wired reflectively,
        // exactly like MockBukkit does for its test plugins).
        Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Object unsafe = unsafeField.get(null);
        Method allocate = unsafe.getClass().getMethod("allocateInstance", Class.class);
        return (BigCasares) allocate.invoke(unsafe, BigCasares.class);
    }

    private static void setBukkitServer(Server value) throws Exception {
        // Bukkit.setServer is a strict one-shot singleton (it refuses to
        // replace an existing server and logs through it), so the static field
        // is wired directly and restored per test.
        Field serverField = Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, value);
    }

    private Server server(FakePlayer... players) {
        return proxy(Server.class, (method, args) -> {
            switch (method.getName()) {
                case "getPlayerExact" -> {
                    if (args.length == 1) {
                        for (FakePlayer player : players) {
                            if (player.name.equals(args[0])) {
                                return player.player;
                            }
                        }
                    }
                    return null;
                }
                case "getOnlinePlayers" -> {
                    return java.util.Arrays.stream(players).map(player -> player.player).toList();
                }
                case "getItemFactory" -> {
                    return itemFactory();
                }
                default -> {
                    return defaultValue(method.getReturnType());
                }
            }
        });
    }

    private Object itemFactory() {
        return proxy(ItemFactory.class, (method, args) -> {
            switch (method.getName()) {
                case "getItemMeta" -> {
                    return new FakeItemMeta().meta;
                }
                case "isApplicable" -> {
                    return true;
                }
                case "asMetaFor" -> {
                    // setItemMeta0 stores asMetaFor(meta, stack): keep the same
                    // fake meta so the PDC written during creation is readable.
                    return args[0];
                }
                default -> {
                    return defaultValue(method.getReturnType());
                }
            }
        });
    }

    private final class FakePlayer {

        final String name;
        final List<String> messages = new ArrayList<>();
        final List<ItemStack> received = new ArrayList<>();
        final Player player;

        FakePlayer(String name) {
            this.name = name;
            this.player = proxy(Player.class, (method, args) -> {
                switch (method.getName()) {
                    case "getName" -> {
                        return name;
                    }
                    case "hasPermission", "isOp" -> {
                        return true;
                    }
                    case "sendMessage" -> {
                        if (args.length > 0 && args[0] instanceof String text) {
                            messages.add(text);
                        }
                        return null;
                    }
                    case "getInventory" -> {
                        return proxy(PlayerInventory.class, (inventoryMethod, inventoryArgs) -> {
                            if ("addItem".equals(inventoryMethod.getName())) {
                                if (inventoryArgs.length > 0 && inventoryArgs[0] instanceof ItemStack[] stacks) {
                                    received.addAll(Arrays.asList(stacks));
                                }
                                return new HashMap<Integer, ItemStack>();
                            }
                            return defaultValue(inventoryMethod.getReturnType());
                        });
                    }
                    default -> {
                        return defaultValue(method.getReturnType());
                    }
                }
            });
        }

        void reset() {
            messages.clear();
            received.clear();
        }
    }
    private static final class FakeItemMeta {

        final Map<String, Object> persistentData = new HashMap<>();
        final ItemMeta meta;
        int maxStackSize = 1;

        FakeItemMeta() {
            final ItemMeta[] holder = new ItemMeta[1];
            holder[0] = (ItemMeta) Proxy.newProxyInstance(
                ItemMeta.class.getClassLoader(),
                new Class<?>[]{ItemMeta.class, Damageable.class},
                (instance, method, args) -> {
                    args = args == null ? new Object[0] : args;
                switch (method.getName()) {
                    case "getPersistentDataContainer" -> {
                        return persistentDataContainer(persistentData);
                    }
                    case "setMaxStackSize" -> {
                        maxStackSize = (Integer) args[0];
                        return null;
                    }
                    case "getMaxStackSize" -> {
                        return maxStackSize;
                    }
                    case "equals" -> {
                        return args.length == 1 && args[0] instanceof ItemMeta;
                    }
                    case "clone" -> {
                        return holder[0];
                    }
                    default -> {
                        return defaultValue(method.getReturnType());
                    }
                }
                }
            );
            this.meta = holder[0];
        }
    }

    private static Object persistentDataContainer(Map<String, Object> data) {
        return proxy(PersistentDataContainer.class, (method, args) -> {
            switch (method.getName()) {
                case "set" -> {
                    data.put(((NamespacedKey) args[0]).toString(), args[2]);
                    return null;
                }
                case "get", "getOrDefault" -> {
                    Object value = data.get(((NamespacedKey) args[0]).toString());
                    if (value == null && "getOrDefault".equals(method.getName()) && args.length > 2) {
                        return args[2];
                    }
                    return value;
                }
                case "has" -> {
                    return data.containsKey(((NamespacedKey) args[0]).toString());
                }
                case "getKeys" -> {
                    return data.keySet();
                }
                case "isEmpty" -> {
                    return data.isEmpty();
                }
                default -> {
                    return defaultValue(method.getReturnType());
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (instance, method, args) -> invocation.invoke(method, args == null ? new Object[0] : args)
        );
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(Method method, Object[] args) throws Throwable;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        return 0.0d;
    }
}
