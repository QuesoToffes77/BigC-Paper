package dev.linqfy.bigCasares.modules.smokebomb;

import dev.linqfy.bigCasares.BigCasares;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class SmokeConcealmentService {

    private final BigCasares plugin;
    private final Map<UUID, Boolean> previousInvisibleState = new LinkedHashMap<>();

    public SmokeConcealmentService(BigCasares plugin) {
        this.plugin = plugin;
    }

    public void conceal(LivingEntity entity) {
        previousInvisibleState.putIfAbsent(entity.getUniqueId(), entity.isInvisible());
        entity.setInvisible(true);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getUniqueId().equals(entity.getUniqueId())) {
                viewer.hideEntity(plugin, entity);
            }
        }
    }

    public void refresh(LivingEntity entity) {
        if (previousInvisibleState.containsKey(entity.getUniqueId())) {
            entity.setInvisible(true);
        }
    }

    public void reveal(UUID entityId) {
        Entity entity = Bukkit.getEntity(entityId);
        Boolean wasInvisible = previousInvisibleState.remove(entityId);
        if (entity instanceof LivingEntity living && wasInvisible != null && !wasInvisible) {
            living.setInvisible(false);
        }
        if (entity != null) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                viewer.showEntity(plugin, entity);
            }
        }
    }

    public void hideFromViewer(Player viewer) {
        for (UUID entityId : previousInvisibleState.keySet()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && !viewer.getUniqueId().equals(entityId)) {
                viewer.hideEntity(plugin, entity);
            }
        }
    }

    public void revealAll() {
        for (UUID entityId : previousInvisibleState.keySet().toArray(UUID[]::new)) {
            reveal(entityId);
        }
    }
}
