package dev.linqfy.bigCasares.items.catalog;

public final class ItemCatalogReloadMessageFormatter {

    private ItemCatalogReloadMessageFormatter() {
    }

    public static String format(ItemCatalogReloadResult result) {
        return switch (result.status()) {
            case SUCCESS -> "§aCatálogo de items recargado: " + result.newCatalog().size()
                + " definiciones, revisión " + result.newCatalog().revision() + ".";
            case NO_OP -> "§eEl catálogo de items no cambió.";
            case ALREADY_RUNNING -> "§eYa hay una recarga de items en curso.";
            case FAILED -> "§cNo se pudo recargar el catálogo de items: "
                + safeMessage(result.failure());
        };
    }

    private static String safeMessage(Throwable failure) {
        String message = failure == null ? null : failure.getMessage();
        return message == null || message.isBlank() ? "error interno" : message;
    }
}
