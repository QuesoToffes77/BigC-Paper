package dev.linqfy.bigCasares.modules.celular;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * F changes the video because the Java client waits for the server before swapping hands, so the phone does not bob.
 * Java's drop (Q) is predicted client-side and would play the re-equip animation; other keys never reach the server.
 * Bedrock controllers use D-pad down (drop) and the emote button (a swap through Geyser's EmoteOffhand extension).
 */
final class CelularListener implements Listener {

    private final CelularService service;
    private final CelularItems items;
    private final CelularVillages villages;
    private final NamespacedKey recipeKey;
    private final Predicate<UUID> bedrock;
    private final Map<UUID, Integer> lastInventoryClick = new HashMap<>();

    CelularListener(CelularService service, CelularItems items, CelularVillages villages, NamespacedKey recipeKey,
                    Predicate<UUID> bedrock) {
        this.service = service;
        this.items = items;
        this.villages = villages;
        this.recipeKey = recipeKey;
        this.bedrock = bedrock;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (recipeKey != null) {
            event.getPlayer().discoverRecipe(recipeKey);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastInventoryClick.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack phone = player.getInventory().getItemInMainHand();
        if (!items.isCelular(phone)) {
            return;
        }
        event.setCancelled(true);
        int index = service.onSwapHands(items.video(phone), player.isSneaking(), isBedrock(player));
        items.setVideo(phone, index);
        player.getInventory().setItemInMainHand(phone);
        showVideo(player, index, "");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        lastInventoryClick.put(event.getWhoClicked().getUniqueId(), Bukkit.getCurrentTick());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack phone = event.getItemDrop().getItemStack();
        if (!items.isCelular(phone) || !service.dropChangesVideo(isBedrock(player), player.isSneaking())) {
            return;
        }
        Integer click = lastInventoryClick.get(player.getUniqueId());
        if (click != null && click == Bukkit.getCurrentTick()) {
            return;
        }
        event.setCancelled(true);
        int index = service.next(items.video(phone));
        items.setVideo(phone, index);
        event.getItemDrop().setItemStack(phone);
        showVideo(player, index, "");
    }

    @EventHandler(ignoreCancelled = true)
    public void onHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItem(event.getNewSlot());
        if (items.isCelular(held)) {
            showVideo(player, items.video(held), service.hint(isBedrock(player)));
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

    private boolean isBedrock(Player player) {
        return bedrock.test(player.getUniqueId());
    }

    private void showVideo(Player player, int index, String hint) {
        player.sendActionBar(Component.text(service.actionBar(index), NamedTextColor.WHITE)
            .append(Component.text(hint, NamedTextColor.GRAY)));
    }
}
