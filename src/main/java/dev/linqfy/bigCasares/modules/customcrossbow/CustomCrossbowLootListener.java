package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.LootGenerateEvent;

import java.util.concurrent.ThreadLocalRandom;

public final class CustomCrossbowLootListener implements Listener {

    private final CustomCrossbowSettings settings;
    private final EchoArrowItem echoArrow;

    public CustomCrossbowLootListener(CustomCrossbowSettings settings, EchoArrowItem echoArrow) {
        this.settings = settings;
        this.echoArrow = echoArrow;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getLootTable() == null) {
            return;
        }
        EchoShardLootPolicy.removeAncientCityEchoShards(event.getLootTable().getKey(), event.getLoot());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Warden)) {
            return;
        }

        int amount = ThreadLocalRandom.current().nextInt(settings.wardenDropMin(), settings.wardenDropMax() + 1);
        event.getDrops().add(echoArrow.createItemStack(amount));
    }
}
