package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import dev.linqfy.bigCasares.platform.ClientEntityPresentationRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

final class SahurOwnedEntities implements AutoCloseable {
    private final JavaModelGateway models;
    private final Map<Entity, JavaModelHandle> entities = new LinkedHashMap<>();
    private final Map<BukkitTask, Boolean> tasks = new LinkedHashMap<>();
    private boolean closed;

    SahurOwnedEntities(JavaModelGateway models) {
        this.models = java.util.Objects.requireNonNull(models, "models");
    }

    synchronized JavaModelHandle ownModel(Entity entity, String modelKey) {
        if (closed) throw new IllegalStateException("Sahur entities are closed");
        JavaModelHandle handle = models.attach(entity, modelKey);
        entities.put(entity, handle);
        return handle;
    }

    synchronized void own(Entity entity) {
        if (closed) {
            entity.remove();
            return;
        }
        entities.putIfAbsent(entity, null);
    }

    synchronized void own(BukkitTask task) {
        if (closed) {
            task.cancel();
            return;
        }
        tasks.put(task, Boolean.TRUE);
    }

    synchronized void release(Entity entity) {
        JavaModelHandle handle = entities.remove(entity);
        if (handle != null) models.close(handle);
        ClientEntityPresentationRegistry.unregister(entity.getUniqueId());
        if (entity.isValid()) entity.remove();
    }

    synchronized void release(BukkitTask task) {
        tasks.remove(task);
        if (!task.isCancelled()) task.cancel();
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        for (BukkitTask task : new ArrayList<>(tasks.keySet())) {
            if (!task.isCancelled()) task.cancel();
        }
        tasks.clear();
        for (Map.Entry<Entity, JavaModelHandle> entry : new ArrayList<>(entities.entrySet())) {
            if (entry.getValue() != null) models.close(entry.getValue());
            ClientEntityPresentationRegistry.unregister(entry.getKey().getUniqueId());
            if (entry.getKey().isValid()) entry.getKey().remove();
        }
        entities.clear();
    }
}
