package dev.linqfy.bigCasares.modules.model;

import org.bukkit.entity.Entity;

public interface JavaModelGateway {
    JavaModelHandle attach(Entity anchor, String modelKey);

    boolean animate(JavaModelHandle handle, String animationKey);

    void close(JavaModelHandle handle);
}
