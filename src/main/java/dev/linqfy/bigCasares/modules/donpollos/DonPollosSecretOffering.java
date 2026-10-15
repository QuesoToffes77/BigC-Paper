package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Set;

/**
 * Lo que hay que darle al Don Pollo Gordito para que te cuente el secreto (y con el, el bloque que invoca al boss).
 * Sin servidor: el producto es un DonPollosProduct o el nombre de un material de vanilla.
 */
enum DonPollosSecretOffering {
    SALSA(DonPollosProduct.SALSA, null, 1, "§f§lSalsa"),
    PICANTE(DonPollosProduct.PICANTE, null, 1, "§c§lPicante"),
    POLLO_FRITO(DonPollosProduct.POLLO_FRITO, null, 64, "§6§lPollo Frito"),
    DIAMANTES(null, "DIAMOND", 5, "§b§lDiamantes");

    /** Item de Don Pollos que pide, o null si es de vanilla. */
    final DonPollosProduct product;
    /** Material de vanilla que pide, o null si es un item de Don Pollos. */
    final String material;
    final int amount;
    final String label;

    DonPollosSecretOffering(DonPollosProduct product, String material, int amount, String label) {
        this.product = product;
        this.material = material;
        this.amount = amount;
        this.label = label;
    }

    /** "1 Salsa", "64 Pollo Frito"... (sin colores), para los mensajes. */
    String plain() {
        return amount + " " + label.replaceAll("§.", "");
    }

    /** Se puede saber el secreto cuando se dio todo. */
    static boolean ready(Set<DonPollosSecretOffering> placed) {
        return placed.size() == values().length;
    }
}
