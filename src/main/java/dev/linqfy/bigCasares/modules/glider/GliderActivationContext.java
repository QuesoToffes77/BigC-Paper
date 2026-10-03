package dev.linqfy.bigCasares.modules.glider;

public record GliderActivationContext(
    boolean equippedGlider,
    boolean onGround,
    double verticalVelocity,
    boolean sneaking,
    boolean swimming,
    boolean vehicle,
    boolean elytra,
    boolean spectator,
    boolean creativeFlying,
    boolean gameModeAllowed,
    GliderActivationMode activationMode,
    double minimumFallSpeed
) {
    public GliderActivationContext withSwimming(boolean value) {
        return copy(value, vehicle, elytra, spectator, creativeFlying);
    }

    public GliderActivationContext withVehicle(boolean value) {
        return copy(swimming, value, elytra, spectator, creativeFlying);
    }

    public GliderActivationContext withElytra(boolean value) {
        return copy(swimming, vehicle, value, spectator, creativeFlying);
    }

    public GliderActivationContext withSpectator(boolean value) {
        return copy(swimming, vehicle, elytra, value, creativeFlying);
    }

    public GliderActivationContext withCreativeFlying(boolean value) {
        return copy(swimming, vehicle, elytra, spectator, value);
    }

    private GliderActivationContext copy(
        boolean newSwimming,
        boolean newVehicle,
        boolean newElytra,
        boolean newSpectator,
        boolean newCreativeFlying
    ) {
        return new GliderActivationContext(
            equippedGlider, onGround, verticalVelocity, sneaking, newSwimming, newVehicle,
            newElytra, newSpectator, newCreativeFlying, gameModeAllowed, activationMode, minimumFallSpeed
        );
    }
}
