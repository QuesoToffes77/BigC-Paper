package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.nio.file.Path;

public final class JavaResourcePackBuilder {
    public Path build(Path javaRoot, Path outputDirectory) throws IOException {
        Path output = outputDirectory.resolve("bigcasares-java.zip");
        DeterministicZipWriter.write(javaRoot, output);
        return output;
    }
}
