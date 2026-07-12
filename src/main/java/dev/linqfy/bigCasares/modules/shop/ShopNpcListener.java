package dev.linqfy.bigCasares.modules.shop;

import io.papermc.paper.entity.LookAnchor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.function.BiConsumer;

public final class ShopNpcListener implements Listener {
    private final ShopNpcFactory factory;
    private final BiConsumer<Player, String> openShop;

    public ShopNpcListener(ShopNpcFactory factory, BiConsumer<Player, String> openShop) {
        this.factory = factory;
        this.openShop = openShop;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        factory.shopId(event.getEntity()).ifPresent(shopId -> {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player player) {
                openShop.accept(player, shopId);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent event) {
        factory.shopId(event.getRightClicked()).ifPresent(shopId -> {
            event.setCancelled(true);
            org.bukkit.Location eyes = event.getPlayer().getEyeLocation();
            event.getRightClicked().lookAt(eyes.getX(), eyes.getY(), eyes.getZ(), LookAnchor.EYES);
            openShop.accept(event.getPlayer(), shopId);
        });
    }
}
