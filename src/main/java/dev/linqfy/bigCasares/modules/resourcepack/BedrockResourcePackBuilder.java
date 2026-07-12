package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.nio.file.Path;

public final class BedrockResourcePackBuilder {
    public Path build(Path bedrockRoot, Path outputDirectory) throws IOException {
        Path output = outputDirectory.resolve("bigcasares-bedrock.mcpack");
        DeterministicZipWriter.write(bedrockRoot, output);
        return output;
    }
}
