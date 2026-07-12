package dev.linqfy.bigCasares.modules.model;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter;
import kr.toxicity.model.api.tracker.EntityTracker;
import org.bukkit.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class BetterModelJavaModelGateway implements JavaModelGateway {
    private final TrackerPort trackerPort;
    private final Map<JavaModelHandle, TrackerSession> sessions = new HashMap<>();

    BetterModelJavaModelGateway() {
        this(new BetterModelTrackerPort());
    }

    BetterModelJavaModelGateway(TrackerPort trackerPort) {
        this.trackerPort = Objects.requireNonNull(trackerPort, "trackerPort");
    }

    @Override
    public JavaModelHandle attach(Entity anchor, String modelKey) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(modelKey, "modelKey");

        var handle = new JavaModelHandle(anchor.getUniqueId(), modelKey);
        sessions.computeIfAbsent(handle, ignored -> trackerPort.attach(anchor, modelKey));
        return handle;
    }

    @Override
    public boolean animate(JavaModelHandle handle, String animationKey) {
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(animationKey, "animationKey");

        var session = sessions.get(handle);
        return session != null && session.animate(animationKey);
    }

    @Override
    public void close(JavaModelHandle handle) {
        Objects.requireNonNull(handle, "handle");

        var session = sessions.remove(handle);
        if (session != null) {
            session.close();
        }
    }

}

final class BetterModelTrackerPort implements TrackerPort {
    private final OwnedTrackerCreator trackerCreator;

    BetterModelTrackerPort() {
        this(BetterModelTrackerPort::createOwnedTracker);
    }

    BetterModelTrackerPort(OwnedTrackerCreator trackerCreator) {
        this.trackerCreator = Objects.requireNonNull(trackerCreator, "trackerCreator");
    }

    @Override
    public TrackerSession attach(Entity anchor, String modelKey) {
        return trackerCreator.create(anchor, modelKey);
    }

    private static TrackerSession createOwnedTracker(Entity anchor, String modelKey) {
        EntityTracker tracker = BetterModel.model(modelKey)
                .map(renderer -> renderer.create(BukkitAdapter.adapt(anchor)))
                .orElseThrow(() -> new IllegalStateException("BetterModel model not loaded: " + modelKey));
        return new BetterModelTrackerSession(tracker);
    }

    private record BetterModelTrackerSession(EntityTracker tracker) implements TrackerSession {
        @Override
        public boolean animate(String animationKey) {
            return tracker.animate(animationKey);
        }

        @Override
        public void close() {
            tracker.close();
        }
    }
}

@FunctionalInterface
interface OwnedTrackerCreator {
    TrackerSession create(Entity anchor, String modelKey);
}

interface TrackerPort {
    TrackerSession attach(Entity anchor, String modelKey);
}

interface TrackerSession {
    boolean animate(String animationKey);

    void close();
}
