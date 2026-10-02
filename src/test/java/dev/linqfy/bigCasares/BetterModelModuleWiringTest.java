package dev.linqfy.bigCasares;

import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import dev.linqfy.bigCasares.modules.nexus.NexusModule;
import dev.linqfy.bigCasares.modules.pveboss.PveBossModule;
import dev.linqfy.bigCasares.modules.teams.TeamId;
import dev.linqfy.bigCasares.platform.ClientPlatform;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BetterModelModuleWiringTest {

    @Test
    void nexusAndBossModulesAcceptTheSharedModelGateway() {
        JavaModelGateway models = new RecordingJavaModelGateway();
        Function<UUID, Optional<TeamId>> playerTeams = ignored -> Optional.empty();
        Function<TeamId, Optional<String>> teamNames = ignored -> Optional.empty();
        Function<UUID, Boolean> resourcePackLoaded = ignored -> false;

        assertDoesNotThrow(() -> new NexusModule(null, playerTeams, teamNames, models));
        assertDoesNotThrow(() -> new PveBossModule(null, ignored -> ClientPlatform.JAVA, resourcePackLoaded, models));
    }

    private static final class RecordingJavaModelGateway implements JavaModelGateway {
        @Override
        public JavaModelHandle attach(Entity anchor, String modelKey) {
            throw new UnsupportedOperationException("No model attachment is expected in this wiring test");
        }

        @Override
        public boolean animate(JavaModelHandle handle, String animationKey) {
            throw new UnsupportedOperationException("No model animation is expected in this wiring test");
        }

        @Override
        public boolean animateOnce(JavaModelHandle handle, String animationKey, Runnable onEnd) {
            throw new UnsupportedOperationException("No model animation is expected in this wiring test");
        }

        @Override
        public boolean scale(JavaModelHandle handle, float factor) {
            throw new UnsupportedOperationException("No model scaling is expected in this wiring test");
        }

        @Override
        public void close(JavaModelHandle handle) {
            throw new UnsupportedOperationException("No model closure is expected in this wiring test");
        }
    }
}
