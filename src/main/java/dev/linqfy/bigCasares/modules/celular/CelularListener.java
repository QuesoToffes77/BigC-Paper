package dev.linqfy.bigCasares.modules.celular;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * F changes the video because the client waits for the server before swapping hands, so the phone does not bob.
 * Drop (Q) is predicted client-side and would play the re-equip animation; other keys never reach the server.
 */
final class CelularListener implements Listener {

    static final String HINT = "  ·  F siguiente, Shift+F anterior";

    private final CelularService service;
    private final CelularItems items;
    private final CelularVillages villages;
    private final NamespacedKey recipeKey;

    CelularListener(CelularService service, CelularItems items, CelularVillages villages, NamespacedKey recipeKey) {
        this.service = service;
        this.items = items;
        this.villages = villages;
        this.recipeKey = recipeKey;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (recipeKey != null) {
            event.getPlayer().discoverRecipe(recipeKey);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack phone = player.getInventory().getItemInMainHand();
        if (!items.isCelular(phone)) {
            return;
        }
        event.setCancelled(true);
        int index = service.navigate(items.video(phone), player.isSneaking());
        items.setVideo(phone, index);
        player.getInventory().setItemInMainHand(phone);
        showVideo(player, index, "");
    }

    @EventHandler(ignoreCancelled = true)
    public void onHold(PlayerItemHeldEvent event) {
        ItemStack held = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (items.isCelular(held)) {
            showVideo(event.getPlayer(), items.video(held), HINT);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoot(LootGenerateEvent event) {
        NamespacedKey table = event.getLootTable().getKey();
        if (!CelularService.isVillageChest(table.getNamespace(), table.getKey())) {
            return;
        }
        Optional<GeneratedStructure> village = villages.villageAt(event.getLootContext().getLocation());
        if (service.shouldAddVillagePhone(table.getNamespace(), table.getKey(), villages.alreadyGave(village))) {
            event.getLoot().add(items.create());
            villages.markGave(village);
        }
    }

    private void showVideo(Player player, int index, String hint) {
        player.sendActionBar(Component.text(service.actionBar(index), NamedTextColor.WHITE)
            .append(Component.text(hint, NamedTextColor.GRAY)));
    }
}
