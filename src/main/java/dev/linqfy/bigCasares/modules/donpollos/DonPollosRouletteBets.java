package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Locale;

/**
 * Que se puede apostar en la ruleta. Pagar x2 / x20 copia el objeto tal cual, asi que se rechaza todo lo que
 * duplicaria contenido guardado o items unicos: cajas de shulker, bundles y items con datos de plugins (PDC) o
 * nombre personalizado.
 */
final class DonPollosRouletteBets {

    private DonPollosRouletteBets() {
    }

    static boolean allowed(String materialName, boolean hasPluginData, boolean hasCustomName) {
        String name = materialName.toUpperCase(Locale.ROOT);
        if (name.endsWith("SHULKER_BOX") || name.endsWith("BUNDLE")) {
            return false;
        }
        return !hasPluginData && !hasCustomName;
    }
}
