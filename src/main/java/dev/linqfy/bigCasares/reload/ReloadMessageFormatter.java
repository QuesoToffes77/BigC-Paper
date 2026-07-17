package dev.linqfy.bigCasares.reload;

public final class ReloadMessageFormatter {

    private ReloadMessageFormatter() {
    }

    public static String format(ReloadResult result) {
        return switch (result.status()) {
            case SUCCESS -> "§aRecarga completada: " + result.activeModules() + "/"
                + result.registeredModules() + " módulos activos.";
            case NO_OP -> "§eNo había cambios para recargar.";
            case ALREADY_RUNNING -> "§eYa hay una recarga en curso.";
            case FAILED -> failureMessage(result.failurePhase());
        };
    }

    private static String failureMessage(ReloadPhase phase) {
        String runtimeState = switch (phase) {
            case PREPARATION -> "El runtime anterior sigue activo";
            case INTERNAL -> "El estado del runtime es incierto";
            default -> "El runtime quedó detenido";
        };
        return "§cLa recarga falló durante " + phaseName(phase) + ". "
            + runtimeState + "; revisá la consola.";
    }

    private static String phaseName(ReloadPhase phase) {
        return switch (phase) {
            case PREPARATION -> "la preparación";
            case SHUTDOWN -> "el apagado de módulos";
            case CLEANUP -> "la limpieza del runtime";
            case CONFIGURATION -> "la carga de configuración";
            case CANDIDATE_ENABLE -> "la activación del runtime";
            case COMMAND_BINDING -> "el registro de comandos";
            case INTERNAL -> "la coordinación interna";
        };
    }
}
