package dev.linqfy.bigCasares.modules.customcrossbow;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;
import org.bukkit.entity.Player;

public final class CustomCrossbowPacketUseListener extends PacketListenerAbstract {

    private final CustomCrossbowChargeListener chargeListener;

    public CustomCrossbowPacketUseListener(CustomCrossbowChargeListener chargeListener) {
        this.chargeListener = chargeListener;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.USE_ITEM) {
            return;
        }

        WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);
        if (packet.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (player != null && chargeListener.tryStartLoading(player)) {
            event.setCancelled(true);
        }
    }
}
