package dev.linqfy.bigCasares.modules.geyser;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GeyserFormResponseDispatcherTest {

    @Test
    void queuesFormResponsesInsteadOfRunningThemOnTheGeyserThread() {
        AtomicReference<Runnable> queued = new AtomicReference<>();
        AtomicInteger selectedButton = new AtomicInteger(-1);
        UUID playerId = UUID.randomUUID();
        BedrockShopForm form = new BedrockShopForm(
            "Shop",
            "Elegí",
            List.of(new BedrockShopForm.Button("Piedra", null)),
            (respondingPlayer, button) -> {
                assertEquals(playerId, respondingPlayer);
                selectedButton.set(button);
            }
        );
        GeyserFormResponseDispatcher dispatcher = new GeyserFormResponseDispatcher(queued::set);

        dispatcher.dispatch(form, playerId, 0);

        assertEquals(-1, selectedButton.get());
        assertNotNull(queued.get());

        queued.get().run();

        assertEquals(0, selectedButton.get());
    }
}
