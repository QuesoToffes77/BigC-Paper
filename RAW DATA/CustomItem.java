package com.copperapple.items;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Interfaz base para todos los ítems personalizados del plugin.
 *
 * ─── SEGURIDAD ────────────────────────────────────────────────────────────────
 * La verificación de identidad se realiza en DOS capas:
 *   1. PersistentDataContainer (PDC): escrito server-side, IMPOSIBLE de falsificar
 *      por jugadores. Es la fuente de verdad para la lógica de efectos.
 *   2. CustomModelData: solo usado para el resource pack (visual). No se usa
 *      como criterio de seguridad de forma aislada.
 *
 * ─── EXTENSIBILIDAD ───────────────────────────────────────────────────────────
 * Para agregar un nuevo ítem personalizado:
 *   1. Crear clase que implemente esta interfaz
 *   2. Implementar getCustomModelData(), getNamespacedKey(), buildItemStack(), onConsume()
 *   3. En buildItemStack(): escribir el PDC con meta.getPersistentDataContainer().set(...)
 *   4. Registrar en CustomItemRegistry
 */
public interface CustomItem {

    /**
     * CustomModelData único. Debe coincidir con el override en el resource pack.
     * Usar solo para fines visuales, NO como único criterio de identidad.
     */
    int getCustomModelData();

    /**
     * NamespacedKey PDC de este ítem.
     * Esta clave se escribe en el PersistentDataContainer al crear el ítem
     * y se lee al consumirlo para verificar autenticidad.
     */
    NamespacedKey getNamespacedKey();

    /**
     * Construye el ItemStack listo para entregar/craftear.
     * DEBE escribir el PDC con getNamespacedKey().
     */
    ItemStack buildItemStack();

    /**
     * Lógica ejecutada al consumir el ítem.
     *
     * @param player El jugador que consumió el ítem.
     * @param item   El ItemStack consumido (ya verificado como auténtico).
     */
    void onConsume(Player player, ItemStack item);

    /**
     * Verifica si un ItemStack es auténtico usando el PersistentDataContainer.
     *
     * ORDEN DE VALIDACIÓN:
     *   1. null check + hasItemMeta()
     *   2. PDC contiene la NamespacedKey correcta con valor 1
     *   3. CustomModelData coincide (defensa en profundidad)
     *
     * @param item El ItemStack a verificar.
     * @return true SOLO si el ítem tiene el PDC correcto Y el CustomModelData correcto.
     */
    default boolean matches(ItemStack item) {
        // Capa 1: null safety
        if (item == null || !item.hasItemMeta()) return false;

        var meta = item.getItemMeta();

        // Capa 2 (PRINCIPAL): verificar PersistentDataContainer
        // Solo el servidor puede escribir PDC → imposible de falsificar
        if (!meta.getPersistentDataContainer().has(getNamespacedKey(), PersistentDataType.INTEGER)) {
            return false;
        }
        Integer pdcValue = meta.getPersistentDataContainer().get(getNamespacedKey(), PersistentDataType.INTEGER);
        if (pdcValue == null || pdcValue.intValue() != 1) return false;

        // Capa 3 (DEFENSA EN PROFUNDIDAD): verificar CustomModelData
        if (!meta.hasCustomModelData()) return false;
        return meta.getCustomModelData() == getCustomModelData();
    }
}
