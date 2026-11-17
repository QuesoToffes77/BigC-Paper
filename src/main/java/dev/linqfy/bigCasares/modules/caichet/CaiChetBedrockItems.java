package dev.linqfy.bigCasares.modules.caichet;

import java.util.List;

/**
 * Los objetos del modulo tal como los necesita la integracion con Geyser: en Bedrock cada uno se ve con su propio
 * dibujo en lugar del objeto base. El icono es la clave que el pack Bedrock de BigCasares genera para la entrada
 * {@code caichet-<id>} de {@code resourcepack/shared/registry.yml}.
 */
public final class CaiChetBedrockItems {

    /**
     * Un objeto: el objeto vanilla sobre el que esta hecho, su item_model en Java y el nombre que se ve.
     */
    public record Item(String javaBaseItem, String itemModel, String icon, String displayName) {
    }

    /** El mismo orden y los mismos ids que usan las fabricas de objetos del modulo. */
    private static final List<Item> ALL = List.of(
        item("stick", "arma", "Arma de Fuego"),
        item("paper", "botella", "Botella de Agua de Almendras"),
        item("paper", "jarra", "Jarra de Memorias"),
        item("paper", "llave", "Llave"),
        item("clock", "reloj_suenos", "Reloj de los Sueños"),
        item("totem_of_undying", "despertar", "Despertar"));

    private CaiChetBedrockItems() {
    }

    public static List<Item> all() {
        return ALL;
    }

    private static Item item(String base, String id, String name) {
        return new Item("minecraft:" + base, "caichet:" + id, "bigcasares.caichet_" + id, name);
    }
}
