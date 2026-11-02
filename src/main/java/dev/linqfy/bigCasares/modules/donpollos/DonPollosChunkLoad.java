package dev.linqfy.bigCasares.modules.donpollos;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Esperar a que carguen los chunks de una ciudad sin que el trabajo sobreviva al modulo (sin servidor).
 * Minecraft completa la carga en otro hilo y puede hacerlo despues de apagar o recargar el modulo. {@code guard}
 * es el del ciclo de vida del modulo (en BigCasares, el de RuntimeGeneration): lo que envuelve no corre si el modulo
 * ya se apago, y apagarlo espera a que termine lo que ya estaba corriendo. Por eso se protegen los dos pasos: el
 * aviso de que cargaron (no programa nada nuevo) y la tarea en el hilo principal (no encola ni pone tickets).
 */
final class DonPollosChunkLoad {

    private DonPollosChunkLoad() {
    }

    static void whenLoaded(CompletableFuture<?> loading, UnaryOperator<Runnable> guard, Consumer<Runnable> mainThread,
                           Consumer<Throwable> failed, Runnable work) {
        loading.whenComplete((ignored, error) -> guard.apply(() -> mainThread.accept(guard.apply(() -> {
            if (error != null) {
                failed.accept(error);
            } else {
                work.run();
            }
        }))).run());
    }
}
