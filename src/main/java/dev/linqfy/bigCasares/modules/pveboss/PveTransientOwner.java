package dev.linqfy.bigCasares.modules.pveboss;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PveTransientOwner {
    private final Map<Object, OwnedResource> resources = new LinkedHashMap<>();
    private boolean active = true;

    void own(Object key, String resource, Runnable cleanup) {
        Objects.requireNonNull(key, "key");
        OwnedResource owned = new OwnedResource(resource, cleanup);
        synchronized (this) {
            if (active) {
                if (resources.putIfAbsent(key, owned) != null) {
                    throw new IllegalArgumentException("Resource key is already owned");
                }
                return;
            }
        }
        owned.cleanup().run();
    }

    boolean release(Object key) {
        OwnedResource owned;
        synchronized (this) {
            owned = resources.remove(key);
        }
        if (owned == null) {
            return false;
        }
        owned.cleanup().run();
        return true;
    }

    synchronized boolean forget(Object key) {
        return resources.remove(key) != null;
    }

    Runnable guard(Runnable callback) {
        Objects.requireNonNull(callback, "callback");
        return () -> runIfActive(callback);
    }

    synchronized boolean isActive() {
        return active;
    }

    synchronized int ownedCount() {
        return resources.size();
    }

    List<PveTransientCleanupFailure> close() {
        List<OwnedResource> owned;
        synchronized (this) {
            if (!active) {
                return List.of();
            }
            active = false;
            owned = new ArrayList<>(resources.values());
            resources.clear();
        }

        List<PveTransientCleanupFailure> failures = new ArrayList<>();
        for (int index = owned.size() - 1; index >= 0; index--) {
            OwnedResource resource = owned.get(index);
            try {
                resource.cleanup().run();
            } catch (Throwable exception) {
                failures.add(new PveTransientCleanupFailure(resource.name(), exception));
            }
        }
        return List.copyOf(failures);
    }

    private synchronized void runIfActive(Runnable callback) {
        if (active) {
            callback.run();
        }
    }

    private record OwnedResource(String name, Runnable cleanup) {

        private OwnedResource {
            name = Objects.requireNonNull(name, "name");
            cleanup = Objects.requireNonNull(cleanup, "cleanup");
        }
    }
}
