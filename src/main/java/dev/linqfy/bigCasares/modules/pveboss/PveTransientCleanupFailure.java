package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Objects;

record PveTransientCleanupFailure(String resource, Throwable cause) {

    PveTransientCleanupFailure {
        resource = Objects.requireNonNull(resource, "resource");
        cause = Objects.requireNonNull(cause, "cause");
    }
}
