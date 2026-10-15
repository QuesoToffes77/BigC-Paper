package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Registro en disco de los bosses cuyo premio ya se cobro (premios-cobrados.yml). Se guarda en el momento, asi un
 * crash antes de que el chunk del boss se guarde no lo deja cobrable otra vez.
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
    public synchronized void add(UUID bossId) {
        if (claimed.add(bossId)) {
            save();
        }
    }

    @Override
    public synchronized void remove(UUID bossId) {
        if (claimed.remove(bossId)) {
            save();
        }
    }

    private void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        List<String> ids = new ArrayList<>();
        claimed.forEach(id -> ids.add(id.toString()));
        yaml.set("cobrados", ids);
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException exception) {
            logger.warning("No se pudo guardar " + file.getName() + ": " + exception.getMessage());
        }
    }
}
