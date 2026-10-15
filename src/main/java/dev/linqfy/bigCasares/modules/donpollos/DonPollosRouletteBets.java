package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Locale;

/**
 * Que se puede apostar en la ruleta (allowlist). Pagar x2 / x20 copia el objeto tal cual, asi que solo se aceptan
 * items simples: sin ningun item meta (encantamientos, nombre, lore, datos de libro/pocion/fuegos, contenido de
 * contenedores, PDC). Las cajas de shulker y los bundles se rechazan aunque esten vacios.
 */
final class DonPollosRouletteBets {

    private DonPollosRouletteBets() {
    }

    static boolean allowed(String materialName, boolean hasItemMeta) {
        String name = materialName.toUpperCase(Locale.ROOT);
        if (name.endsWith("SHULKER_BOX") || name.endsWith("BUNDLE")) {
            return false;
        }
        return !hasItemMeta;
    }
}
