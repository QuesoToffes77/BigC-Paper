package dev.linqfy.bigCasares.modules.resourcepack;

import java.nio.file.Path;

public interface JavaPackLayerProvider {

    String id();

    Path archive();

    static JavaPackLayerProvider fixed(String id, Path archive) {
        return new JavaPackLayerProvider() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Path archive() {
                return archive;
            }
        };
    }
}
