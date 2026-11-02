package dev.linqfy.bigCasares.modules.donpollos;

import dev.linqfy.bigCasares.module.runtime.RuntimeGeneration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosChunkLoadTest {

    private final RuntimeGeneration generation = new RuntimeGeneration(1);
    private final List<Runnable> mainThread = new ArrayList<>();
    private final AtomicInteger built = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();

    private CompletableFuture<Void> load() {
        CompletableFuture<Void> chunks = new CompletableFuture<>();
        DonPollosChunkLoad.whenLoaded(chunks, generation::guard, mainThread::add, error -> failures.incrementAndGet(),
            built::incrementAndGet);
        return chunks;
    }

    @Test
    void whileTheModuleIsOnTheCityIsQueued() {
        load().complete(null);
        assertEquals(1, mainThread.size());
        mainThread.getFirst().run();
        assertEquals(1, built.get());
    }

    @Test
    void chunksThatFinishLoadingAfterShutdownScheduleNothing() {
        // se retienen los chunks, se apaga el modulo y recien despues terminan de cargar
        CompletableFuture<Void> chunks = load();
        generation.retire();
        chunks.complete(null);
        assertTrue(mainThread.isEmpty(), "no se programa ninguna tarea");
        assertEquals(0, built.get(), "no se encola la ciudad ni se ponen tickets");
    }

    @Test
    void aTaskAlreadyScheduledDoesNothingIfTheModuleTurnsOffBeforeItRuns() {
        load().complete(null);
        generation.retire();
        mainThread.getFirst().run();
        assertEquals(0, built.get());
    }

    @Test
    void loadErrorsAreReportedOnlyWhileTheModuleIsOn() {
        load().completeExceptionally(new IllegalStateException("chunk roto"));
        mainThread.getFirst().run();
        assertEquals(1, failures.get());
        assertEquals(0, built.get());

        CompletableFuture<Void> late = load();
        generation.retire();
        late.completeExceptionally(new IllegalStateException("chunk roto"));
        assertEquals(1, mainThread.size());
        assertEquals(1, failures.get());
    }
}
