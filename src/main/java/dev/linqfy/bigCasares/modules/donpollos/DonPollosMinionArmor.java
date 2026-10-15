package dev.linqfy.bigCasares.modules.donpollos;

import java.util.ArrayList;
import java.util.List;

/**
 * Armadura de los bichos del Don Pollo Boss segun cuantas veces ya le gano quien lo invoco (sin Bukkit).
 * Primera vez: como siempre. Despues cobre, hierro, diamante y netherite; y con netherite, cada vez mejores
 * encantamientos (Proteccion hasta V y despues Espinas hasta III). El Coronel y los Sanders ya pelean con netherite:
 * a ellos los encantamientos les arrancan desde la segunda vez.
 */
final class DonPollosMinionArmor {

    enum Tier {
        NONE(null, null),
        COBRE("COPPER", "cobre"),
        HIERRO("IRON", "hierro"),
        DIAMANTE("DIAMOND", "diamante"),
        NETHERITE("NETHERITE", "netherite");

        /** Prefijo del material de las piezas (COPPER_CHESTPLATE...). */
        final String material;
        /** Como termina el modelo con esta armadura (bigcasares_labubu_verde_cobre...). */
        final String suffix;

        Tier(String material, String suffix) {
            this.material = material;
            this.suffix = suffix;
        }

        String piece(String slot) {
            return material + "_" + slot;
        }
    }

    static final int MAX_PROTECTION = 5;
    static final int MAX_THORNS = 3;

    /** Modelos de bichos que tienen version con armadura (tools/generar_armaduras.py). */
    static final List<String> ARMORED_BASE_MODELS = List.of("bigcasares_gallina_wsp", "bigcasares_labubu_verde",
        "bigcasares_labubu_rojo", "bigcasares_labubu_rosa", "bigcasares_labubu_azul");

    private DonPollosMinionArmor() {
    }

    static Tier tier(int level) {
        Tier[] tiers = Tier.values();
        return tiers[Math.max(0, Math.min(level, tiers.length - 1))];
    }

    /** A partir de que nivel arrancan los encantamientos: cuando la armadura ya es de netherite. */
    private static int enchantStart(boolean alwaysNetherite) {
        return alwaysNetherite ? 0 : Tier.NETHERITE.ordinal();
    }

    static int protection(int level, boolean alwaysNetherite) {
        return Math.max(0, Math.min(MAX_PROTECTION, level - enchantStart(alwaysNetherite)));
    }

    static int thorns(int level, boolean alwaysNetherite) {
        return Math.max(0, Math.min(MAX_THORNS, level - enchantStart(alwaysNetherite) - MAX_PROTECTION));
    }

    /** Modelo del bicho: con armadura si tiene version con armadura; si no, el de siempre. */
    static String model(String baseModel, int level) {
        Tier tier = tier(level);
        return tier == Tier.NONE || !ARMORED_BASE_MODELS.contains(baseModel) ? baseModel : baseModel + "_" + tier.suffix;
    }

    static List<String> armoredModels() {
        List<String> models = new ArrayList<>();
        for (String base : ARMORED_BASE_MODELS) {
            for (Tier tier : Tier.values()) {
                if (tier != Tier.NONE) {
                    models.add(base + "_" + tier.suffix);
                }
            }
        }
        return models;
    }
}
