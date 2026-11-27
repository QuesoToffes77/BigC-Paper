package dev.linqfy.bigCasares.modules.forex;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForexRecoveryCommandTest {
    @Test
    void pausedMarketAllowsOnlyAuthorizedStaffToInspectAndAcknowledge(@TempDir Path directory) throws Exception {
        Path forex = Files.createDirectories(directory.resolve("forex"));
        Path journal = forex.resolve("transfers.log");
        Files.writeString(journal, "INTENT pending\n");
        Files.writeString(forex.resolve("market.yml"), "market");
        Plugin plugin = (Plugin) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Plugin.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getDataFolder" -> directory.toFile();
                case "getLogger" -> Logger.getAnonymousLogger();
                default -> null;
            });
        ForexRuntime runtime = new ForexRuntime(plugin, ForexSettings.defaults(), new FakeBank(), ForexTeams.NONE);
        ForexCommand command = new ForexCommand(runtime);
        var details = runtime.reconciliationDetails();
        List<String> messages = new ArrayList<>();
        CommandSender ordinary = sender(false, messages);
        command.onCommand(ordinary, null, "forex", new String[] {"admin", "resolver", details.journalHash(), details.snapshotHash()});
        assertTrue(Files.exists(journal));
        assertTrue(messages.stream().anyMatch(message -> message.contains("permiso")));

        CommandSender admin = sender(true, messages);
        command.onCommand(admin, null, "forex", new String[] {"admin", "pendiente"});
        assertTrue(messages.stream().anyMatch(message -> message.contains(details.journalHash())));
        command.onCommand(admin, null, "forex", new String[] {"admin", "resolver", details.journalHash(), details.snapshotHash()});
        assertFalse(Files.exists(journal));
        assertTrue(runtime.paused());
    }

    private CommandSender sender(boolean permitted, List<String> messages) {
        return (CommandSender) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {CommandSender.class},
            (proxy, method, args) -> {
                if (method.getName().equals("hasPermission")) return permitted;
                if (method.getName().equals("getName")) return "operator";
                if (method.getName().equals("sendMessage") && args != null && args.length == 1
                    && args[0] instanceof String message) messages.add(message);
                return null;
            });
    }
}
