package dev.linqfy.bigCasares.modules.nexus;

import java.util.Set;
import java.util.UUID;

public interface NexusVisualGateway {

    NexusVisualHandle spawn(NexusVisualRequest request);

    void updateHealth(NexusId nexusId, double healthFraction);

    void playDamageAnimation(NexusId nexusId, NexusDamageKind damageKind);

    void playDestroyedAnimation(NexusId nexusId);

    void remove(NexusId nexusId);

    static NexusVisualGateway noop() {
        return new NexusVisualGateway() {
            @Override
            public NexusVisualHandle spawn(NexusVisualRequest request) {
                return new NexusVisualHandle(request.nexusId(), UUID.randomUUID(), Set.of());
            }

            @Override
            public void updateHealth(NexusId nexusId, double healthFraction) {
            }

            @Override
            public void playDamageAnimation(NexusId nexusId, NexusDamageKind damageKind) {
            }

            @Override
            public void playDestroyedAnimation(NexusId nexusId) {
            }

            @Override
            public void remove(NexusId nexusId) {
            }
        };
    }
}
