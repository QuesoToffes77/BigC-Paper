package dev.linqfy.bigCasares.modules.geyser;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Floodgate ships its own unrelocated copy of {@code org.geysermc.event}. A plugin that resolves {@code @Subscribe}
 * from that copy gets every annotated method ignored by Geyser, so the bridge has to subscribe explicitly.
 */
class GeyserEventSubscriptionContractTest {

    private static final Path BRIDGE =
        Path.of("src/main/java/dev/linqfy/bigCasares/modules/geyser/GeyserRuntimeBridge.java");

    @Test
    void bridgeSubscribesToEveryGeyserEventExplicitly() throws IOException {
        String source = Files.readString(BRIDGE);
        assertFalse(source.contains("eventBus().register("), "annotation-based registration breaks with Floodgate");
        assertFalse(java.util.regex.Pattern.compile("(?m)^\\s*@Subscribe\\b").matcher(source).find(),
            "annotated handlers are ignored when Floodgate is installed");
        for (String event : new String[]{"GeyserDefineResourcePacksEvent", "GeyserDefineEntitiesEvent",
            "GeyserDefineCustomItemsEvent", "ServerSpawnEntityEvent"}) {
            assertTrue(source.contains("subscribe(this, " + event + ".class,"), event + " must be subscribed");
        }
        assertTrue(source.contains("unregisterAll(this)"), "closing must drop every subscription");
    }
}
