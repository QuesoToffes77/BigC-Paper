package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Sound;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import dev.linqfy.bigCasares.items.CustomItemRegistry;

public final class AirdropListener implements Listener {

    private final AirdropService service;
    private final AirdropRewardItemFactory rewardFactory;

    private AirdropPosition chestPosition;
    private long unlockAtMillis;
    private Runnable claimedCallback;

    public AirdropListener(
        AirdropService service,
        Map<String, Material> lootMaterials,
        CustomItemRegistry customItems
    ) {
        this.service = service;
        this.rewardFactory = new AirdropRewardItemFactory(customItems, lootMaterials);
    }

    /** Called after a player claims the chest, so the module can drop its visuals. */
    public void setClaimedCallback(Runnable claimedCallback) {
        this.claimedCallback = claimedCallback;
    }

    public void setChestPosition(AirdropPosition position, long unlockAtMillis) {
        this.chestPosition = position;
        this.unlockAtMillis = unlockAtMillis;
    }

    public void clearChestPosition() {
        this.chestPosition = null;
        this.unlockAtMillis = 0L;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (chestPosition == null) return;
        if (!service.isActive()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (clicked.getType() != Material.CHEST) return;
        if (!chestPosition.matchesBlock(clicked.getX(), clicked.getY(), clicked.getZ())) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        long remainingLockMillis = unlockAtMillis - System.currentTimeMillis();
        if (remainingLockMillis > 0L) {
            player.sendActionBar("§c🔒 Este Airdrop se desbloquea en §f" + formatDuration(remainingLockMillis));
            player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.8f, 0.75f);
            return;
        }
        AirdropData current = service.getCurrentDrop().orElse(null);
        List<AirdropReward> loot = service.getLootForCurrent();
        List<ItemStack> rewards = rewardFactory.createAll(loot);
        if (!AirdropInventoryCapacity.canFit(
            player.getInventory().getStorageContents(), rewards, player.getInventory().getMaxStackSize())) {
            player.sendActionBar("§cLiberá espacio en tu inventario para reclamar este Airdrop.");
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 0.8f);
            return;
        }
        if (!service.claim()) {
            return;
        }

        fillPlayerInventory(player, rewards);
        clicked.setType(Material.AIR);
        chestPosition = null;
        if (claimedCallback != null) {
            claimedCallback.run();
        }

        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.15f);
        player.sendTitle("§a§lAIRDROP RECLAMADO",
            "§7" + (current == null ? "Recompensa obtenida"
                : current.quality().color() + current.quality().displayName()
                    + " §7- " + current.type().displayName()), 5, 35, 10);
        player.getServer().broadcastMessage("§a✔ " + player.getName() + " reclamó el airdrop de "
            + (current == null ? "suministros" : current.type().displayName() + " ["
                + current.quality().color() + current.quality().displayName() + "§a]") + "!");
    }

    @EventHandler
    public void onChestBreak(BlockBreakEvent event) {
        if (!isTrackedChest(event.getBlock())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendActionBar("§6✈ Reclamá este Airdrop interactuando con el cofre.");
    }

    private static String formatDuration(long remainingMillis) {
        long totalSeconds = Math.max(0L, (remainingMillis + 999L) / 1000L);
        return "%02d:%02d".formatted(totalSeconds / 60L, totalSeconds % 60L);
    }

    private void fillPlayerInventory(Player player, List<ItemStack> rewards) {
        Inventory inv = player.getInventory();
        for (ItemStack reward : rewards) {
            if (!inv.addItem(reward).isEmpty()) {
                throw new IllegalStateException("Airdrop inventory capacity changed during claim");
            }
        }
    }

    private boolean isTrackedChest(Block block) {
        return chestPosition != null
            && block.getType() == Material.CHEST
            && chestPosition.matchesBlock(block.getX(), block.getY(), block.getZ());
    }
}
