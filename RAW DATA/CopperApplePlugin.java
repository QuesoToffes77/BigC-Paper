package com.copperapple;

import com.copperapple.commands.CopperAppleCommand;
import com.copperapple.items.CopperAppleItem;
import com.copperapple.items.CustomItemRegistry;
import com.copperapple.listeners.CraftListener;
import com.copperapple.listeners.ItemConsumeListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin principal: CopperApple
 * Añade ítems personalizados seguros usando CustomModelData + PersistentDataContainer.
 * Compatible con Paper/Spigot para Minecraft Java Edition 1.20+.
 *
 * Seguridad: el ítem se identifica por PDC (no falsificable por jugadores),
 *            respaldado por CustomModelData para compatibilidad con el resource pack.
 */
public class CopperApplePlugin extends JavaPlugin {

    // ─── Singleton ────────────────────────────────────────────────────────────
    private static CopperApplePlugin instance;

    // ─── NamespacedKey PDC ────────────────────────────────────────────────────
    /**
     * Clave PDC para identificar la Manzana de Cobre.
     * Se escribe en el ItemMeta al crear el ítem y se verifica al consumirlo.
     * Los jugadores NO pueden manipular el PDC; es 100% server-side.
     */
    private NamespacedKey copperAppleKey;

    // ─── Registro de ítems ────────────────────────────────────────────────────
    private CustomItemRegistry itemRegistry;

    // ─── Clave de receta (para poder eliminarla en onDisable si es necesario) ─
    private NamespacedKey recipeKey;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onEnable() {
        instance = this;

        // Inicializar las NamespacedKeys ANTES que el registro (los ítems las necesitan)
        this.copperAppleKey = new NamespacedKey(this, "copper_apple");
        this.recipeKey      = new NamespacedKey(this, "copper_apple_recipe");

        // Inicializar el registro de ítems (pasa las keys a los constructores)
        this.itemRegistry = new CustomItemRegistry(this);

        // Registrar la receta de crafteo con shape tipo golden apple
        registerCopperAppleRecipe();

        // Registrar listeners y comandos
        registerListeners();
        registerCommands();

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║   CopperApple Plugin activado!   ║");
        getLogger().info("║   Items registrados: " + itemRegistry.getItemCount() + "           ║");
        getLogger().info("╚══════════════════════════════════╝");
    }

    @Override
    public void onDisable() {
        // Eliminar la receta del servidor para evitar duplicados al recargar
        Bukkit.removeRecipe(recipeKey);
        getLogger().info("CopperApple Plugin desactivado.");
    }

    // ─── Registro de receta de crafteo ────────────────────────────────────────

    /**
     * Registra la receta shaped de la Manzana de Cobre.
     *
     * Patrón (como la golden apple pero con cobre):
     *   C C C
     *   C A C
     *   C C C
     * Donde:  C = COPPER_INGOT   A = APPLE
     *
     * IMPORTANTE: el resultado es el ItemStack completo con PDC y CustomModelData,
     * no un ítem nuevo sin metadatos. Se obtiene del registro para garantizar
     * que siempre sea idéntico al que entrega /copperapple.
     */
    private void registerCopperAppleRecipe() {
        // Obtener el ItemStack auténtico del registro (con PDC + CMD)
        ItemStack result = itemRegistry
            .findByModelData(CopperAppleItem.MODEL_DATA)
            .orElseThrow(() -> new IllegalStateException(
                "CopperAppleItem no encontrado en el registro al registrar receta."
            ))
            .buildItemStack();

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("CCC", "CAC", "CCC");
        recipe.setIngredient('C', Material.COPPER_BLOCK);
        recipe.setIngredient('A', Material.APPLE);

        Bukkit.addRecipe(recipe);
        getLogger().info("Receta de Manzana de Cobre registrada correctamente.");
    }

    // ─── Listeners ────────────────────────────────────────────────────────────

    /**
     * Registra todos los listeners del plugin.
     * CraftListener intercepta PrepareItemCraftEvent para blindar el resultado.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ItemConsumeListener(this), this);
        getServer().getPluginManager().registerEvents(new CraftListener(this), this);
    }

    // ─── Comandos ─────────────────────────────────────────────────────────────

    private void registerCommands() {
    CopperAppleCommand cmd = new CopperAppleCommand(this);

    if (getCommand("copperapple") != null) {
        getCommand("copperapple").setExecutor(cmd);
        getCommand("copperapple").setTabCompleter(cmd);
    }
}
    // ─── Getters ──────────────────────────────────────────────────────────────

    public static CopperApplePlugin getInstance() { return instance; }

    /**
     * NamespacedKey PDC de la Manzana de Cobre.
     * Usar esta clave para leer/escribir el PDC en todos los ítems del plugin.
     */
    public NamespacedKey getCopperAppleKey() { return copperAppleKey; }

    /** Clave de la receta (usada también en CraftListener para validar). */
    public NamespacedKey getRecipeKey() { return recipeKey; }

    public CustomItemRegistry getItemRegistry() { return itemRegistry; }
}
