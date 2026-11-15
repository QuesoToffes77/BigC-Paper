package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.dimension.datapack.DatapackInstaller;
import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Installs the mythic-dungeon datapack into {@code <level>/datapacks/bigcasares-mythic-dungeons}
 * (the main world, like the dimension module). Worldgen registries are read only at startup,
 * so a new or changed pack needs a server restart and only affects chunks generated afterwards.
 * Failures are logged and reported, never thrown.
 */
public final class MythicDatapackInstaller {

    private static final String PREFIX = "[MythicDungeons] ";
    private static final Object LOCK = new Object();
    private static boolean observed;
    private static String startupHash;

    /**
     * @param installed      the pack is on disk and current
     * @param changed        the pack was written or replaced in this call
     * @param restartPending the running server did not load this pack version at startup
     */
    public record InstallReport(boolean installed, boolean changed, boolean restartPending, String contentHash,
                                Path directory, String error) {
    }

    private MythicDatapackInstaller() {
    }

    /** Builds the code-only pack and installs it. */
    public static InstallReport install(JavaPlugin plugin) {
        return install(plugin, List.of());
    }

    /**
     * Builds the pack with extra template sources shipped in the plugin and installs it. A source
     * that throws or supplies invalid pieces is skipped with a warning.
     */
    public static InstallReport install(JavaPlugin plugin, List<MythicTemplateSource> sources) {
        Logger logger = plugin.getLogger();
        try {
            Path datapacks = plugin.getServer().getLevelDirectory().resolve("datapacks");
            Path backups = plugin.getDataFolder().toPath().resolve("mythicdungeons").resolve("datapack-backups");
            String loadedAtStartup = observeStartup(
                DatapackInstaller.installedHash(datapacks.resolve(MythicDungeonDatapack.FOLDER)).orElse(null));
            GeneratedDatapack pack;
            try {
                // the normal case: every source is fine, so the pack is built once
                pack = MythicDungeonDatapack.build(sources);
            } catch (RuntimeException broken) {
                pack = MythicDungeonDatapack.build(usableSources(sources, logger));
            }
            return install(pack, datapacks, backups, loadedAtStartup, logger, Clock.systemDefaultZone());
        } catch (RuntimeException exception) {
            logger.log(Level.SEVERE, PREFIX + "No se pudo generar el datapack de mazmorras mitologicas", exception);
            return new InstallReport(false, false, false, null, null, String.valueOf(exception.getMessage()));
        }
    }

    /** Keeps the sources whose pieces build on their own; broken ones are logged and dropped. */
    static List<MythicTemplateSource> usableSources(List<MythicTemplateSource> sources, Logger logger) {
        List<MythicTemplateSource> usable = new ArrayList<>();
        for (MythicTemplateSource source : sources) {
            try {
                MythicDungeonDatapack.build(List.of(source));
                usable.add(source);
            } catch (RuntimeException exception) {
                logger.log(Level.WARNING, PREFIX + "Fuente de plantillas descartada (" + source.getClass().getName()
                    + "): " + exception.getMessage(), exception);
            }
        }
        return usable;
    }

    /** Pure core: installs {@code pack} and logs (in Spanish) whether a restart is needed. */
    static InstallReport install(GeneratedDatapack pack, Path datapacks, Path backups, String loadedAtStartup,
                                 Logger logger, Clock clock) {
        Objects.requireNonNull(pack, "pack");
        boolean restartPending = !pack.contentHash().equals(loadedAtStartup);
        try {
            DatapackInstaller.InstallResult result = new DatapackInstaller(clock)
                .install(pack, datapacks, MythicDungeonDatapack.FOLDER, backups);
            if (result.changed()) {
                logger.warning(PREFIX + "Datapack de mazmorras mitologicas "
                    + (result.outcome() == DatapackInstaller.Outcome.INSTALLED ? "instalado" : "actualizado")
                    + " en " + result.directory() + " (" + pack.size() + " archivos). REINICIA el servidor para que"
                    + " se generen las mazmorras: el worldgen solo se lee al arrancar y solo afecta a chunks nuevos.");
            } else if (restartPending) {
                logger.warning(PREFIX + "El datapack de mazmorras mitologicas ya esta instalado pero el servidor"
                    + " arranco con otra version: hace falta reiniciar.");
            }
            return new InstallReport(true, result.changed(), restartPending, pack.contentHash(), result.directory(), null);
        } catch (IOException | RuntimeException exception) {
            logger.log(Level.SEVERE, PREFIX + "No se pudo instalar el datapack de mazmorras mitologicas en "
                + datapacks + "; el plugin sigue sin las estructuras nuevas.", exception);
            return new InstallReport(false, false, false, pack.contentHash(),
                datapacks.resolve(MythicDungeonDatapack.FOLDER), String.valueOf(exception.getMessage()));
        }
    }

    /** Hash on disk when this JVM first looked, i.e. the pack the server loaded; survives plugin reloads. */
    private static String observeStartup(String hashOnDisk) {
        synchronized (LOCK) {
            if (!observed) {
                observed = true;
                startupHash = hashOnDisk;
            }
            return startupHash;
        }
    }
}
