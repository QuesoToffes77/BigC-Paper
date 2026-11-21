package dev.linqfy.bigCasares.modules.model;

import dev.linqfy.bigCasares.testsupport.CompiledClass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterModelAssetInstallerContractTest {

    // BetterModel classes cannot be loaded in unit tests, so the wiring is checked from the compiled bytecode.

    @Test
    void installerAsksBetterModelToReloadTheCopiedModels() throws Exception {
        CompiledClass installer = CompiledClass.of(getClass(), "dev.linqfy.bigCasares.modules.model.BetterModelAssetInstaller");

        assertTrue(installer.calls("kr/toxicity/model/api/BetterModelPlatform", "reload"));
        String source = java.nio.file.Files.readString(java.nio.file.Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/model/BetterModelAssetInstaller.java"));
        org.junit.jupiter.api.Assertions.assertFalse(source.contains("if (changed)"),
            "BetterModel reload must not depend on copied bytes changing");
    }

    @Test
    void pveModuleRepeatsInstallAfterBetterModelDelayedInitialization() throws Exception {
        CompiledClass module = CompiledClass.of(getClass(), "dev.linqfy.bigCasares.modules.pveboss.PveBossModule");

        assertTrue(module.calls("dev/linqfy/bigCasares/modules/model/BetterModelAssetInstaller", "installSahurModels"));
        assertTrue(module.calls("org/bukkit/scheduler/BukkitScheduler", "runTaskLater"));
    }
}
