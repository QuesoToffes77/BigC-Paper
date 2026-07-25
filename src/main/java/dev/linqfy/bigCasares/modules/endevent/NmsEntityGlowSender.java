package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Sends Minecraft's shared entity metadata flags to one viewer. This keeps the
 * hunter wallhack client-side instead of changing the target's server state.
 */
final class NmsEntityGlowSender {
    private final JavaPlugin plugin;
    private ReflectionAccess access;
    private boolean unavailableReported;

    NmsEntityGlowSender(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    boolean send(Player viewer, Entity target, boolean glowing) {
        try {
            if (access == null) {
                access = ReflectionAccess.load();
            }
            access.send(viewer, target, glowing);
            return true;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            if (!unavailableReported) {
                unavailableReported = true;
                plugin.getLogger().warning(
                    "No se pudo enviar el glow NMS por jugador: " + failure.getMessage()
                );
            }
            return false;
        }
    }

    private record ReflectionAccess(
        Method craftEntityGetHandle,
        Method craftPlayerGetHandle,
        Field sharedFlagsAccessor,
        Method entityGetId,
        Method entityGetData,
        Method entityDataGet,
        Method dataValueCreate,
        Constructor<?> metadataPacketConstructor,
        Field playerConnection,
        Method connectionSend
    ) {
        private static ReflectionAccess load() throws ReflectiveOperationException {
            Class<?> craftEntity = Class.forName("org.bukkit.craftbukkit.entity.CraftEntity");
            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            Class<?> nmsEntity = Class.forName("net.minecraft.world.entity.Entity");
            Class<?> serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> entityDataAccessor =
                Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
            Class<?> synchedEntityData =
                Class.forName("net.minecraft.network.syncher.SynchedEntityData");
            Class<?> dataValue =
                Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue");
            Class<?> packet =
                Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
            Class<?> connection =
                Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl");

            Field sharedFlags = nmsEntity.getDeclaredField("DATA_SHARED_FLAGS_ID");
            sharedFlags.setAccessible(true);

            return new ReflectionAccess(
                craftEntity.getMethod("getHandle"),
                craftPlayer.getMethod("getHandle"),
                sharedFlags,
                nmsEntity.getMethod("getId"),
                nmsEntity.getMethod("getEntityData"),
                synchedEntityData.getMethod("get", entityDataAccessor),
                dataValue.getMethod("create", entityDataAccessor, Object.class),
                packet.getConstructor(int.class, List.class),
                serverPlayer.getField("connection"),
                connection.getMethod("send", packetInterface)
            );
        }

        private void send(Player viewer, Entity target, boolean glowing)
            throws ReflectiveOperationException {
            Object targetHandle = craftEntityGetHandle.invoke(target);
            Object accessor = sharedFlagsAccessor.get(null);
            Object entityData = entityGetData.invoke(targetHandle);
            byte currentFlags = (Byte) entityDataGet.invoke(entityData, accessor);
            byte sentFlags = EntityGlowFlags.withGlow(currentFlags, glowing);
            Object packedValue = dataValueCreate.invoke(null, accessor, sentFlags);
            Object packet = metadataPacketConstructor.newInstance(
                entityGetId.invoke(targetHandle), List.of(packedValue)
            );

            Object viewerHandle = craftPlayerGetHandle.invoke(viewer);
            Object connection = playerConnection.get(viewerHandle);
            connectionSend.invoke(connection, packet);
        }
    }
}
