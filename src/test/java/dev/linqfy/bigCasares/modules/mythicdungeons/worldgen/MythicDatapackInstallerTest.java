package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MythicDatapackInstallerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void installsOnceAndAsksForARestart(@TempDir Path root) {
        GeneratedDatapack pack = MythicDungeonDatapack.build();
        List<String> messages = new ArrayList<>();
        Logger logger = capturing(messages);
        Path datapacks = root.resolve("world/datapacks");

        MythicDatapackInstaller.InstallReport first = MythicDatapackInstaller.install(pack, datapacks, root.resolve("backups"),
            null, logger, CLOCK);
        assertTrue(first.installed() && first.changed() && first.restartPending());
        assertTrue(Files.isRegularFile(datapacks.resolve(MythicDungeonDatapack.FOLDER).resolve("pack.mcmeta")));
        assertTrue(messages.stream().anyMatch(message -> message.contains("REINICIA")));

        MythicDatapackInstaller.InstallReport second = MythicDatapackInstaller.install(pack, datapacks, root.resolve("backups"),
            pack.contentHash(), logger, CLOCK);
        assertTrue(second.installed());
        assertFalse(second.changed());
        assertFalse(second.restartPending());
    }

    @Test
    void ioFailureIsReportedNotThrown(@TempDir Path root) throws Exception {
        Path blocked = root.resolve("datapacks");
        Files.writeString(blocked, "not a directory");
        List<String> messages = new ArrayList<>();
        MythicDatapackInstaller.InstallReport report = MythicDatapackInstaller.install(MythicDungeonDatapack.build(), blocked,
            root.resolve("backups"), null, capturing(messages), CLOCK);
        assertFalse(report.installed());
        assertNotNull(report.error());
        assertTrue(messages.stream().anyMatch(message -> message.contains("No se pudo instalar")));
    }

    private static Logger capturing(List<String> messages) {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
        return logger;
    }
}
