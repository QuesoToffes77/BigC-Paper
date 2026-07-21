package dev.linqfy.bigCasares.modules.model;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterModelAssetInstallerContractTest {

    @Test
    void reloadsEvenWhenInstalledAssetsAlreadyMatchToRepairAStaleGeneratedPack() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/model/BetterModelAssetInstaller.java"
        ));

        assertTrue(source.contains("BetterModel.platform().reload()"));
        assertFalse(source.contains("if (changed)"), "BetterModel reload must not depend on copied bytes changing");
    }

    @Test
    void pveModuleRepeatsInstallAfterBetterModelDelayedInitialization() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/pveboss/PveBossModule.java"
        ));

        assertTrue(source.contains("runTaskLater(plugin"));
        assertTrue(source.contains("BetterModelAssetInstaller.installSahurModels(plugin)"));
        assertTrue(source.contains("60L"), "Delayed reload must run after BetterModel startup tasks");
    }
}
