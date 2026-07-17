package dev.linqfy.bigCasares.modules.resourcepack;

public final class PackReloadException extends RuntimeException {

    private final PackReloadPhase phase;

    public PackReloadException(PackReloadPhase phase, Throwable cause) {
        super(cause);
        this.phase = java.util.Objects.requireNonNull(phase, "phase");
    }

    public PackReloadPhase phase() {
        return phase;
    }
}
