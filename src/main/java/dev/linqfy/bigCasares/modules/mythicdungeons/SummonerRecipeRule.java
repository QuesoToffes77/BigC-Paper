package dev.linqfy.bigCasares.modules.mythicdungeons;

import java.util.List;
import java.util.Optional;

/**
 * La regla del crafteo del Invocador (sin servidor): exactamente cuatro Fragmentos de Odio del mismo jefe y nada mas.
 * La receta real es la del catalogo ({@code boss_summoner_<cultura>.yml}, sin forma, con ingredientes exactos);
 * esta regla la vuelve a comprobar en la mesa por si otra receta o un item vanilla parecido se cuela.
 */
public final class SummonerRecipeRule {

    public static final int FRAGMENTS_PER_SUMMONER = 4;

    private SummonerRecipeRule() {
    }

    /**
     * @param gridItemIds el id de catalogo de cada casilla ocupada de la mesa; {@code null} (o vacio) para un item
     *                    que no es del catalogo
     * @return la cultura cuyo Invocador sale, si la mesa cumple la regla
     */
    public static Optional<MythicCulture> result(List<String> gridItemIds) {
        if (gridItemIds == null || gridItemIds.size() != FRAGMENTS_PER_SUMMONER) {
            return Optional.empty();
        }
        MythicCulture culture = null;
        for (String itemId : gridItemIds) {
            Optional<MythicCulture> fragment = MythicCulture.byFragmentItemId(itemId);
            if (fragment.isEmpty() || (culture != null && culture != fragment.get())) {
                return Optional.empty();
            }
            culture = fragment.get();
        }
        return Optional.ofNullable(culture);
    }

    /** Si el id es uno de los items del modulo (fragmento o invocador). */
    public static boolean isMythicItem(String itemId) {
        return MythicCulture.byFragmentItemId(itemId).isPresent()
            || MythicCulture.bySummonerItemId(itemId).isPresent();
    }
}
