package dev.linqfy.bigCasares.modules.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BetterModelAssetInstaller {
    private static final List<String> SAHUR_ASSETS = List.of(
        "boss_sahur.bbmodel",
        "bat_boss.bbmodel",
        "sakur.png"
    );

    private BetterModelAssetInstaller() { }

    public static void installSahurModels(JavaPlugin plugin) {
        Path models = BetterModel.platform().dataFolder().toPath().resolve("models");
        try {
            Files.createDirectories(models);
            for (String fileName : SAHUR_ASSETS) {
                String resource = "bettermodel/models/" + fileName;
                byte[] packaged;
                try (InputStream stream = plugin.getResource(resource)) {
                    if (stream == null) throw new IllegalStateException("Missing packaged BetterModel asset: " + resource);
                    packaged = stream.readAllBytes();
                }
                Path target = models.resolve(fileName);
                if (!Files.isRegularFile(target) || !java.util.Arrays.equals(packaged, Files.readAllBytes(target))) {
                    Files.write(target, packaged);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not install Sahur BetterModel assets", exception);
        }
        BetterModelPlatform.ReloadResult result = BetterModel.platform().reload();
        if (result instanceof BetterModelPlatform.ReloadResult.Failure failure) {
            throw new IllegalStateException("BetterModel could not reload Sahur assets", failure.throwable());
        }
        if (BetterModel.model("boss_sahur").isEmpty() || BetterModel.model("bat_boss").isEmpty()) {
            throw new IllegalStateException("BetterModel did not load boss_sahur and bat_boss");
        }
    }
}
