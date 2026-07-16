package dev.linqfy.bigCasares.modules.resourcepack;

public final class PackReloadMessageFormatter {

    private PackReloadMessageFormatter() {
    }

    public static String format(PackReloadResult result) {
        return switch (result.status()) {
            case SUCCESS -> "§aPack Java activado: " + result.newManifest().version()
                + ". Bedrock requiere reinicio.";
            case NO_OP -> "§eEl pack final no cambió; no se reenvió.";
            case ALREADY_RUNNING -> "§eYa hay una construcción de pack en curso ("
                + shortJob(result.jobId()) + ").";
            case CANCELLED -> "§eLa construcción del pack fue cancelada porque el runtime se retiró.";
            case FAILED -> "§cLa recarga del pack falló durante " + phase(result.failurePhase())
                + "; el pack anterior sigue activo. Revisá la consola.";
        };
    }

    private static String phase(PackReloadPhase phase) {
        return switch (phase) {
            case DISCOVERY -> "el descubrimiento";
            case BUILD -> "la construcción o validación";
            case PUBLICATION -> "la publicación";
            case COMMIT -> "la activación";
            case INTERNAL -> "la coordinación interna";
        };
    }

    private static String shortJob(java.util.UUID jobId) {
        return jobId.toString().substring(0, 8);
    }
}
