package dev.linqfy.bigCasares.modules.model;

import java.util.Objects;
import java.util.UUID;

public record JavaModelHandle(UUID anchorId, String modelKey) {

    public JavaModelHandle {
        Objects.requireNonNull(anchorId, "anchorId");
        Objects.requireNonNull(modelKey, "modelKey");
    }
}
