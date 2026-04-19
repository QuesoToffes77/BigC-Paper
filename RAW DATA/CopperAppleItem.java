package com.copperapple.items;

import com.copperapple.CopperApplePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * Implementación de la "Manzana de Cobre".
 *
 * ─── SEGURIDAD ─────────────────────────────────────────────────────────────
 * El ítem se identifica por PersistentDataContainer (PDC), no por nombre/lore/CMD.
 * El PDC es metadata server-side: los jugadores NO pueden modificarlo con anvil,
 * NBT editors del cliente u otros medios sin acceso de OP/consola.
 *
 * ─── EFECTOS ───────────────────────────────────────────────────────────────
 * Al consumirse aplica:
 *   • Regeneración II  — 5 segundos
 *   • Resistencia  I   — 10 segundos
 *   • Velocidad    I   — 5 segundos
 *   • Visual: strikeLightningEffect (sin daño) + ELECTRIC_SPARK + HEART
 */
public class CopperAppleItem implements CustomItem {

    // ─── Constantes ────────────────────────────────────────────────────────────

    /** CustomModelData: debe coincidir con el override en apple.json del resource pack. */
    public static final int MODEL_DATA = 1001;

    private static final String DISPLAY_NAME = "§6Manzana de Cobre";

    private static final List<String> LORE = Arrays.asList(
        "§7Energía conductora",
        "§8[CustomItem #" + MODEL_DATA + "]"
    );

    // ─── Duración de efectos (ticks; 20 ticks = 1 segundo) ────────────────────

    private static final int REGEN_TICKS      = 5  * 20;
    private static final int RESISTANCE_TICKS = 10 * 20;
    private static final int SPEED_TICKS      = 5  * 20;

    // ─── NamespacedKey PDC ────────────────────────────────────────────────────

    /**
     * La NamespacedKey se obtiene del plugin principal para garantizar unicidad
     * y consistencia. Se inyecta en el constructor.
     */
    private final NamespacedKey namespacedKey;

    public CopperAppleItem(CopperApplePlugin plugin) {
        // Usa la misma key centralizada definida en CopperApplePlugin
        this.namespacedKey = plugin.getCopperAppleKey();
    }

    // ─── Implementación de CustomItem ─────────────────────────────────────────

    @Override
    public int getCustomModelData() {
        return MODEL_DATA;
    }

    @Override
    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    /**
     * Construye el ItemStack auténtico.
     *
     * SIEMPRE escribe:
     *   1. PDC con la NamespacedKey del plugin (valor 1, tipo INTEGER)
     *   2. CustomModelData para el resource pack
     *   3. Nombre y Lore visibles
     *
     * Este método es la ÚNICA fuente de ítems auténticos del plugin.
     * El comando /copperapple y la receta de crafteo usan este método.
     */
    @Override
    public ItemStack buildItemStack() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();

        // ── Metadatos visibles ──
        meta.setDisplayName(DISPLAY_NAME);
        meta.setLore(LORE);
        meta.setCustomModelData(MODEL_DATA);

        // ── PDC: marca de autenticidad (server-side, no falsificable) ──
        // Valor 1 = ítem auténtico generado por este plugin
        meta.getPersistentDataContainer().set(namespacedKey, PersistentDataType.INTEGER, 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Lógica de consumo: efectos de poción + visuales + sonidos + mensaje.
     * Solo se llama cuando matches() retornó true (PDC y CMD verificados).
     */
    @Override
    public void onConsume(Player player, ItemStack item) {
        applyPotionEffects(player);
        applyVisualEffects(player);
        playSoundEffects(player);
        sendFeedbackMessage(player);
    }

    // ─── Métodos auxiliares privados ──────────────────────────────────────────

    /**
     * Aplica los efectos de poción.
     * Los efectos existentes del mismo tipo son reemplazados (sin acumulación).
     */
    private void applyPotionEffects(Player player) {
        // Regeneración II (amplifier 1 = nivel 2) por 5 segundos
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION, REGEN_TICKS,
            1,     // Nivel 2
            false, // No reducir partículas
            true,  // Mostrar partículas
            true   // Mostrar ícono HUD
        ));

        // Resistencia I (amplifier 0 = nivel 1) por 10 segundos
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.RESISTANCE, RESISTANCE_TICKS,
            0, false, true, true
        ));

        // Velocidad I (amplifier 0 = nivel 1) por 5 segundos
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.SPEED, SPEED_TICKS,
            0, false, true, true
        ));
    }

    /**
     * Efectos visuales:
     *   • ELECTRIC_SPARK: partículas eléctricas alrededor del jugador
     *   • strikeLightningEffect: flash visual (SIN daño, SIN incendio)
     *   • HEART: partículas de corazones que indican regeneración
     */
    private void applyVisualEffects(Player player) {
        Location loc = player.getLocation();

        // Partículas eléctricas tipo relámpago (visibles para todos cercanos)
        player.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            loc.clone().add(0, 1, 0),
            30,
            0.4,
            0.7,
            0.4,
            0.05
        );

        // Relámpago visual puro — strikeLightningEffect NO causa daño,
        // NO incendia bloques, NO asusta mobs. Solo es la animación.
        player.getWorld().strikeLightningEffect(loc);

        // Corazones sobre la cabeza (indican la regeneración activa)
        player.getWorld().spawnParticle(
            Particle.HEART,
            loc.clone().add(0, 2.2, 0),
            10,
            0.4, 0.2, 0.4,
            0.0
        );
    }

    /**
     * Efectos de sonido:
     *   • Trueno suavizado (escuchado por todos en rango)
     *   • Encantamiento (personal, indica activación de magia)
     *   • Bebida (personal, feedback de consumo)
     */
    private void playSoundEffects(Player player) {
        Location loc = player.getLocation();

        // Trueno suavizado — escuchado por jugadores cercanos
        player.getWorld().playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.6f);

        // Sonidos personales — solo el jugador que consume los escucha
        player.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        player.playSound(loc, Sound.ENTITY_GENERIC_DRINK,        0.5f, 0.9f);
    }

    private void sendFeedbackMessage(Player player) {
        player.sendMessage("§6⚡ §eManzana de Cobre consumida§6!");
        player.sendMessage("§7  §a✦ Regen II §7(5s)  §b✦ Resistencia I §7(10s)  §e✦ Velocidad I §7(5s)");
    }
}
