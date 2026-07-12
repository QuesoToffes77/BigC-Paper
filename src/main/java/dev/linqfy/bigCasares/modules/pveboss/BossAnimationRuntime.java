package dev.linqfy.bigCasares.modules.pveboss;

public final class BossAnimationRuntime {
    private final BossAnimationDefinition definition;
    private BossAnimationState state;
    private int elapsedTicks;

    public BossAnimationRuntime(BossAnimationDefinition definition) {
        this.definition = definition;
        this.state = BossAnimationState.IDLE;
        this.elapsedTicks = 0;
    }

    public void play() {
        this.state = BossAnimationState.PLAYING;
        this.elapsedTicks = 0;
    }

    public void stop() {
        this.state = BossAnimationState.IDLE;
        this.elapsedTicks = 0;
    }

    public void tick() {
        if (state != BossAnimationState.PLAYING) {
            return;
        }

        elapsedTicks++;

        if (elapsedTicks >= definition.durationTicks()) {
            if (definition.type() == BossAnimationType.LOOPING) {
                elapsedTicks = 0; // Loop seamlessly
            } else {
                state = BossAnimationState.COMPLETED;
            }
        }
    }

    public BossAnimationFrame interpolate() {
        float progress = (definition.durationTicks() == 0) ? 1.0f : (float) elapsedTicks / definition.durationTicks();
        progress = Math.min(1.0f, Math.max(0.0f, progress));

        float scale = definition.startScale() + (definition.endScale() - definition.startScale()) * progress;
        float yOffset = definition.startYOffset() + (definition.endYOffset() - definition.startYOffset()) * progress;
        float yRotation = definition.startYRotation() + (definition.endYRotation() - definition.startYRotation()) * progress;

        return new BossAnimationFrame(scale, yOffset, yRotation);
    }

    public BossAnimationDefinition definition() {
        return definition;
    }

    public BossAnimationState state() {
        return state;
    }
}
