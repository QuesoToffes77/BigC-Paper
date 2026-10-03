package dev.linqfy.bigCasares.modules.glider;

public final class GliderActivationPolicy {

    private GliderActivationPolicy() {
    }

    public static boolean canStart(GliderActivationContext context) {
        if (context == null
            || !context.equippedGlider()
            || context.onGround()
            || context.verticalVelocity() >= context.minimumFallSpeed()
            || context.swimming()
            || context.vehicle()
            || context.elytra()
            || context.spectator()
            || context.creativeFlying()
            || !context.gameModeAllowed()) {
            return false;
        }
        return context.activationMode() == GliderActivationMode.FALLING || context.sneaking();
    }
}
