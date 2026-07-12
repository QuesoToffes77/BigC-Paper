package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BossAnimationService {
    private final Map<UUID, BossAnimationRuntime> activeAnimations = new ConcurrentHashMap<>();
    private final Map<UUID, BossAnimationDefinition> defaultAnimations = new ConcurrentHashMap<>();
    private final BossAnimationGateway gateway;

    public BossAnimationService(BossAnimationGateway gateway) {
        this.gateway = gateway;
    }

    public void setDefaultAnimation(UUID bossId, BossAnimationDefinition definition) {
        defaultAnimations.put(bossId, definition);
        if (!activeAnimations.containsKey(bossId)) {
            playAnimation(bossId, definition);
        }
    }

    public void playAnimation(UUID bossId, BossAnimationDefinition definition) {
        BossAnimationRuntime current = activeAnimations.get(bossId);
        if (current != null && current.definition().id().equals(definition.id())) {
            if (current.state() == BossAnimationState.PLAYING && definition.type() == BossAnimationType.LOOPING) {
                return; // Already playing and looping, don't interrupt
            }
        }

        if (current != null && current.state() == BossAnimationState.PLAYING) {
            if (definition.priority() < current.definition().priority()) {
                return; // Do not override higher priority animation
            }
        }

        BossAnimationRuntime next = new BossAnimationRuntime(definition);
        next.play();
        activeAnimations.put(bossId, next);
        gateway.notifyBedrockAnimation(bossId, definition.id());
    }

    public void stopAnimation(UUID bossId) {
        BossAnimationRuntime current = activeAnimations.get(bossId);
        if (current != null) {
            current.stop();
        }
        returnToDefault(bossId);
    }

    public Optional<BossAnimationFrame> tick(UUID bossId, int updateTicks) {
        BossAnimationRuntime current = activeAnimations.get(bossId);
        if (current == null) {
            return Optional.empty();
        }

        if (current.state() == BossAnimationState.COMPLETED) {
            returnToDefault(bossId);
            current = activeAnimations.get(bossId);
            if (current == null) {
                return Optional.empty();
            }
        }

        for (int i = 0; i < updateTicks; i++) {
            current.tick();
        }

        BossAnimationFrame frame = current.interpolate();
        gateway.applyFrame(bossId, frame, updateTicks);
        return Optional.of(frame);
    }

    public void removeBoss(UUID bossId) {
        activeAnimations.remove(bossId);
        defaultAnimations.remove(bossId);
    }

    public Optional<BossAnimationRuntime> activeAnimation(UUID bossId) {
        return Optional.ofNullable(activeAnimations.get(bossId));
    }

    private void returnToDefault(UUID bossId) {
        BossAnimationDefinition defaultDef = defaultAnimations.get(bossId);
        if (defaultDef != null) {
            BossAnimationRuntime idle = new BossAnimationRuntime(defaultDef);
            idle.play();
            activeAnimations.put(bossId, idle);
            gateway.notifyBedrockAnimation(bossId, defaultDef.id());
        } else {
            activeAnimations.remove(bossId);
        }
    }
}
