package dev.linqfy.bigCasares.reload;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadMessageFormatterTest {

    @Test
    void formatsSuccessfulReloadInGreen() {
        String message = ReloadMessageFormatter.format(ReloadResult.success(List.of(), List.of(), 17, 17));

        assertTrue(message.startsWith("§a"));
        assertTrue(message.contains("17/17"));
        assertTrue(message.contains("completada"));
    }

    @Test
    void formatsBusyReloadWithoutSuccessClaim() {
        String message = ReloadMessageFormatter.format(ReloadResult.alreadyRunning());

        assertTrue(message.startsWith("§e"));
        assertTrue(message.contains("en curso"));
        assertFalse(message.contains("completada"));
    }

    @Test
    void formatsPhaseSpecificFailureWithoutExceptionDetails() {
        IllegalStateException failure = new IllegalStateException("secret backend details");
        ReloadResult result = ReloadResult.failed(
            ReloadPhase.CLEANUP,
            List.of(),
            List.of(),
            failure,
            0,
            17
        );

        String message = ReloadMessageFormatter.format(result);

        assertTrue(message.startsWith("§c"));
        assertTrue(message.contains("limpieza"));
        assertTrue(message.contains("consola"));
        assertFalse(message.contains("secret backend details"));
        assertFalse(message.contains("completada"));
    }

    @Test
    void preparationFailureSaysPreviousRuntimeRemainsActive() {
        ReloadResult result = ReloadResult.failed(
            ReloadPhase.PREPARATION,
            List.of(),
            List.of(),
            new IllegalStateException("bad preparation"),
            17,
            17
        );

        String message = ReloadMessageFormatter.format(result);

        assertTrue(message.contains("runtime anterior sigue activo"));
        assertFalse(message.contains("quedó detenido"));
    }

    @Test
    void internalFailureReportsUncertainRuntimeState() {
        ReloadResult result = ReloadResult.failed(
            ReloadPhase.INTERNAL,
            List.of(),
            List.of(),
            new IllegalStateException("unexpected"),
            0,
            17
        );

        String message = ReloadMessageFormatter.format(result);

        assertTrue(message.contains("estado del runtime es incierto"));
        assertFalse(message.contains("quedó detenido"));
    }
}
