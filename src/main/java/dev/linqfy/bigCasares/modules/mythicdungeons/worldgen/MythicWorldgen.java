package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Punto de entrada de la generacion de las mazmorras (las estructuras {@code bigc_myth:<cultura>} del datapack). El
 * modulo lo llama una vez al prenderse: arma el datapack y lo instala en el mundo principal. Los cambios de worldgen
 * recien se ven despues de reiniciar el servidor y solo en chunks nuevos; el instalador lo avisa en el log.
 */
public final class MythicWorldgen {

    private MythicWorldgen() {
    }

    public static void install(JavaPlugin plugin, BukkitRuntimeRegistrations registrations) {
        install(plugin, registrations, List.of());
    }

    /** Igual que {@link #install(JavaPlugin, BukkitRuntimeRegistrations)} con piezas extra empaquetadas en el plugin. */
    public static void install(JavaPlugin plugin, BukkitRuntimeRegistrations registrations,
                               List<MythicTemplateSource> sources) {
        // instalar nunca tira el modulo abajo: los errores quedan en el log y en el reporte
        MythicDatapackInstaller.install(plugin, sources);
    }
}
