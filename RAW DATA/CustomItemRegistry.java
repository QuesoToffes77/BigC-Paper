package com.copperapple.items;

import com.copperapple.CopperApplePlugin;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro central de todos los ítems personalizados del plugin.
 *
 * ═══════════════════════════════════════════════════════════
 *  CÓMO AGREGAR UN NUEVO ÍTEM PERSONALIZADO:
 * ═══════════════════════════════════════════════════════════
 *  1. Crear clase en "items/" que implemente CustomItem
 *  2. Inyectar CopperApplePlugin en el constructor para la NamespacedKey PDC
 *  3. En buildItemStack(): escribir el PDC y el CustomModelData
 *  4. Agregar register(new MiNuevoItem(plugin)) en el constructor de esta clase
 *  5. Agregar override en apple.json del resource pack
 * ═══════════════════════════════════════════════════════════
 *
 * Detección de ítems:
 *   findByItemStack() usa matches() de CustomItem, que verifica PDC + CMD.
 *   El PDC es la fuente de verdad; CMD es defensa secundaria.
 */
public class CustomItemRegistry {

    /**
     * Mapa CustomModelData → CustomItem.
     * Lookup O(1) por CMD como clave primaria de acceso rápido.
     * La seguridad real la provee PDC dentro de matches().
     */
    private final Map<Integer, CustomItem> registeredItems = new HashMap<>();

    /**
     * Constructor: recibe el plugin para poder pasarlo a los constructores
     * de los ítems, quienes lo usan para obtener su NamespacedKey PDC.
     *
     * @param plugin La instancia del plugin principal.
     */
    public CustomItemRegistry(CopperApplePlugin plugin) {
        // ─── ÍTEMS REGISTRADOS ──────────────────────────────────────────────
        register(new CopperAppleItem(plugin));
        // Para agregar más ítems en el futuro:
        // register(new IronAppleItem(plugin));    // CMD 1002
        // register(new GoldNuggetItem(plugin));   // CMD 1003
        // ────────────────────────────────────────────────────────────────────
    }

    /**
     * Registra un ítem personalizado.
     * Lanza excepción si el CustomModelData ya está en uso (previene conflictos).
     */
    public void register(CustomItem item) {
        int cmd = item.getCustomModelData();
        if (registeredItems.containsKey(cmd)) {
            throw new IllegalArgumentException(
                "CustomModelData " + cmd + " ya está en uso. Cada ítem debe tener un CMD único."
            );
        }
        registeredItems.put(cmd, item);
    }

    /**
     * Busca el CustomItem que corresponde a un ItemStack usando verificación PDC.
     *
     * FLUJO:
     *   1. Null/meta check básico
     *   2. Lookup rápido por CustomModelData (O(1))
     *   3. Verificación completa PDC + CMD vía matches() (defensa en profundidad)
     *
     * Si un jugador forjó un ítem con CMD 1001 pero sin PDC correcto,
     * el lookup encuentra el ítem pero matches() retorna false → seguro.
     *
     * @param itemStack El ItemStack a identificar.
     * @return Optional con el CustomItem si es auténtico, vacío si no.
     */
    public Optional<CustomItem> findByItemStack(ItemStack itemStack) {
        // Null safety y check de metadatos
        if (itemStack == null || !itemStack.hasItemMeta()) return Optional.empty();

        var meta = itemStack.getItemMeta();

        // Lookup rápido por CMD (O(1)) — si no tiene CMD o no hay ítem con ese CMD, salir
        if (!meta.hasCustomModelData()) return Optional.empty();
        CustomItem candidate = registeredItems.get(meta.getCustomModelData());
        if (candidate == null) return Optional.empty();

        // Verificación completa: PDC + CMD (dentro de matches())
        // Si el ítem fue falsificado, matches() retorna false aquí
        if (!candidate.matches(itemStack)) return Optional.empty();

        return Optional.of(candidate);
    }

    /**
     * Busca un CustomItem por su CustomModelData (sin verificación de ítem).
     * Usado internamente para obtener el ítem al registrar recetas o ejecutar comandos.
     */
    public Optional<CustomItem> findByModelData(int modelData) {
        return Optional.ofNullable(registeredItems.get(modelData));
    }

    /** Todos los ítems registrados (para listados, GUIs, etc.). */
    public Collection<CustomItem> getAllItems() {
        return registeredItems.values();
    }

    /** Cantidad de ítems registrados actualmente. */
    public int getItemCount() {
        return registeredItems.size();
    }
}
