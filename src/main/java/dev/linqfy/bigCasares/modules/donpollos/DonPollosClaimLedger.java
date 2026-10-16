package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Registro en disco de los bosses cuyo premio ya se cobro (premios-cobrados.yml). Se guarda en el momento, asi un
 * crash antes de que el chunk del boss se guarde no lo deja cobrable otra vez. Las entradas se conservan incluso al
 * retirar la entidad: remove() no confirma que el chunk haya guardado la eliminacion.
 */
final class DonPollosClaimLedger implements DonPollosBossClaims.Ledger {

    private final File file;
    private final Logger logger;
    private final Set<UUID> claimed = new HashSet<>();

    DonPollosClaimLedger(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        for (String id : YamlConfiguration.loadConfiguration(file).getStringList("cobrados")) {
            try {
                claimed.add(UUID.fromString(id));
            } catch (IllegalArgumentException ignored) {
                // entrada rota: se descarta
            }
        }
    }

    @Override
    public synchronized boolean contains(UUID bossId) {
        return claimed.contains(bossId);
    }

    @Override
    public synchronized boolean add(UUID bossId) {
        if (claimed.contains(bossId)) {
            return true;
        }
        Set<UUID> next = new HashSet<>(claimed);
        next.add(bossId);
        if (!save(next)) {
            return false;
        }
        claimed.add(bossId);
        return true;
    }

    private boolean save(Set<UUID> next) {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> ids = new ArrayList<>();
        next.forEach(id -> ids.add(id.toString()));
        yaml.set("cobrados", ids);
        Path temporary = null;
        try {
            Path target = file.toPath().toAbsolutePath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer bytes = ByteBuffer.wrap(yaml.saveToString().getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) {
                    channel.write(bytes);
                }
                channel.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            logger.warning("No se pudo guardar " + file.getName() + ": " + exception.getMessage());
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    logger.fine("No se pudo quitar el temporal de " + file.getName() + ": " + exception.getMessage());
                }
            }
        }
    }
}
