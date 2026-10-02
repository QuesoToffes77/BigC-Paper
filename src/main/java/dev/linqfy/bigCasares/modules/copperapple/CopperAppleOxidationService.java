package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongSupplier;

public final class CopperAppleOxidationService {

    private static final org.bukkit.NamespacedKey CREATED_AT_KEY =
        new org.bukkit.NamespacedKey("bigcasares", "copper_apple_created_at");
    private static final org.bukkit.NamespacedKey STAGE_KEY =
        new org.bukkit.NamespacedKey("bigcasares", "copper_apple_oxidation_stage");
    private static final long CREATION_TIME_WINDOW_MILLIS = 5_000L;

    private final CustomItemRegistry itemRegistry;
    private final CopperAppleOxidationPolicy policy;
    private final boolean oxidationEnabled;
    private final int maxStackSize;
    private final LongSupplier currentTimeMillis;
    private final Random random;

    public CopperAppleOxidationService(CustomItemRegistry itemRegistry, CopperAppleSettings settings) {
        this(
            itemRegistry,
            settings.oxidationPolicy(),
            settings.oxidationEnabled(),
            settings.maxStackSize(),
            System::currentTimeMillis,
            new Random()
        );
    }

    CopperAppleOxidationService(
        CustomItemRegistry itemRegistry,
        CopperAppleOxidationPolicy policy,
        boolean oxidationEnabled,
        int maxStackSize,
        LongSupplier currentTimeMillis,
        Random random
    ) {
        this.itemRegistry = Objects.requireNonNull(itemRegistry, "itemRegistry");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.oxidationEnabled = oxidationEnabled;
        this.maxStackSize = Math.max(1, Math.min(maxStackSize, 64));
        this.currentTimeMillis = Objects.requireNonNull(currentTimeMillis, "currentTimeMillis");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void initialize(ItemStack stack) {
        refresh(stack);
    }

    public void refreshOnlinePlayers(Collection<? extends Player> players) {
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                refreshInventory(player.getInventory());
                sendHeldItemTimer(player);
            }
        }
    }

    public void refreshInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        for (ItemStack stack : inventory.getContents()) {
            refresh(stack);
        }
        normalizeInventoryAges(inventory);
    }

    public CopperAppleOxidationStage refresh(ItemStack stack) {
        if (!isCopperApple(stack)) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        long now = currentTimeMillis.getAsLong();
        Long createdAt = data.get(CREATED_AT_KEY, PersistentDataType.LONG);
        boolean changed = false;
        if (!oxidationEnabled) {
            createdAt = now;
            if (data.has(CREATED_AT_KEY, PersistentDataType.LONG)) {
                data.remove(CREATED_AT_KEY);
                changed = true;
            }
        } else if (createdAt == null || createdAt < 0L || createdAt > now) {
            createdAt = CopperAppleStackingPolicy.canonicalCreatedAt(now, CREATION_TIME_WINDOW_MILLIS);
            data.set(CREATED_AT_KEY, PersistentDataType.LONG, createdAt);
            changed = true;
        }

        CopperAppleOxidationStage stage = oxidationEnabled
            ? policy.stageAt(createdAt, now)
            : CopperAppleOxidationStage.FRESH;
        String priorStage = data.get(STAGE_KEY, PersistentDataType.STRING);
        List<String> expectedLore = stage.lore();
        if (!stage.name().equals(priorStage) || !stage.itemModel().equals(meta.getItemModel())
            || !stage.displayName().equals(meta.getDisplayName()) || !expectedLore.equals(meta.getLore())) {
            meta.setItemModel(stage.itemModel());
            meta.setDisplayName(stage.displayName());
            meta.setLore(expectedLore);
            data.set(STAGE_KEY, PersistentDataType.STRING, stage.name());
            changed = true;
        }
        if (requiresMaxStackSizeUpdate(meta, maxStackSize)) {
            meta.setMaxStackSize(maxStackSize);
            changed = true;
        }
        if (changed) {
            stack.setItemMeta(meta);
        }
        return stage;
    }

    static boolean requiresMaxStackSizeUpdate(ItemMeta meta, int expectedSize) {
        return !meta.hasMaxStackSize() || meta.getMaxStackSize() != expectedSize;
    }

    public void normalizeForMerge(ItemStack first, ItemStack second) {
        CopperAppleOxidationStage firstStage = refresh(first);
        CopperAppleOxidationStage secondStage = refresh(second);
        if (!oxidationEnabled || firstStage == null || firstStage != secondStage) {
            return;
        }
        long oldest = CopperAppleStackingPolicy.oldestCreatedAt(createdAt(first), createdAt(second));
        if (oldest >= 0L) {
            setCreatedAt(first, oldest);
            setCreatedAt(second, oldest);
        }
    }

    public void normalizeWithInventory(ItemStack incoming, Inventory inventory) {
        refresh(incoming);
        if (inventory == null || !isCopperApple(incoming)) {
            return;
        }
        for (ItemStack existing : inventory.getStorageContents()) {
            normalizeForMerge(incoming, existing);
        }
        normalizeInventoryAges(inventory);
    }

    public void consume(Player player, ItemStack consumedItem) {
        CopperAppleOxidationStage stage = refresh(consumedItem);
        if (stage == null) {
            return;
        }
        CopperAppleEffectProfile profile = CopperAppleBalance.profileFor(stage);
        addEffect(player, PotionEffectType.INSTANT_HEALTH, 1, profile.instantHealthAmplifier());
        addEffect(player, PotionEffectType.ABSORPTION, profile.absorptionTicks(), profile.absorptionAmplifier());
        addEffect(player, PotionEffectType.REGENERATION, profile.regenerationTicks(), profile.regenerationAmplifier());
        addEffect(player, PotionEffectType.SPEED, profile.speedTicks(), profile.speedAmplifier());
        addEffect(player, PotionEffectType.STRENGTH, profile.strengthTicks(), profile.strengthAmplifier());
        addEffect(player, PotionEffectType.RESISTANCE, profile.resistanceTicks(), profile.resistanceAmplifier());

        player.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            player.getLocation().add(0.0, 1.0, 0.0),
            24 + stage.ordinal() * 12,
            0.5,
            0.8,
            0.5,
            0.04
        );
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.15f);
        if (rollLightning(profile.lightningChancePercent())) {
            player.getWorld().strikeLightningEffect(player.getLocation());
        }
    }

    private boolean isCopperApple(ItemStack stack) {
        return stack != null
            && !stack.getType().isAir()
            && itemRegistry.resolveItemId(stack).filter(CopperAppleItem.ID::equals).isPresent();
    }

    private void normalizeInventoryAges(Inventory inventory) {
        if (!oxidationEnabled) {
            return;
        }
        Map<CopperAppleOxidationStage, Long> oldestByStage = new EnumMap<>(CopperAppleOxidationStage.class);
        for (ItemStack stack : inventory.getStorageContents()) {
            CopperAppleOxidationStage stage = stageFrom(stack);
            long createdAt = createdAt(stack);
            if (stage != null && createdAt >= 0L) {
                oldestByStage.merge(stage, createdAt, Math::min);
            }
        }
        for (ItemStack stack : inventory.getStorageContents()) {
            CopperAppleOxidationStage stage = stageFrom(stack);
            if (stage != null && oldestByStage.containsKey(stage)) {
                setCreatedAt(stack, oldestByStage.get(stage));
            }
        }
    }

    private void sendHeldItemTimer(Player player) {
        ItemStack stack = isCopperApple(player.getInventory().getItemInMainHand())
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();
        if (!isCopperApple(stack)) {
            return;
        }
        CopperAppleOxidationStage stage = refresh(stack);
        long remaining = oxidationEnabled
            ? policy.millisUntilNextStage(createdAt(stack), currentTimeMillis.getAsLong())
            : 0L;
        if (!oxidationEnabled) {
            player.sendActionBar("§7Oxidación desactivada");
        } else if (remaining <= 0L) {
            player.sendActionBar("§3Oxidación: §f" + stageLabel(stage) + " §7(máxima)");
        } else {
            player.sendActionBar("§6Oxidación: §f" + stageLabel(stage)
                + " §8| §7Siguiente etapa: §f" + formatDuration(remaining));
        }
    }

    private CopperAppleOxidationStage stageFrom(ItemStack stack) {
        if (!isCopperApple(stack)) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        String stored = meta.getPersistentDataContainer().get(STAGE_KEY, PersistentDataType.STRING);
        if (stored == null) {
            return refresh(stack);
        }
        try {
            return CopperAppleOxidationStage.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return refresh(stack);
        }
    }

    private long createdAt(ItemStack stack) {
        if (!isCopperApple(stack)) {
            return -1L;
        }
        Long stored = stack.getItemMeta().getPersistentDataContainer()
            .get(CREATED_AT_KEY, PersistentDataType.LONG);
        return stored == null ? -1L : stored;
    }

    private void setCreatedAt(ItemStack stack, long createdAt) {
        if (!isCopperApple(stack) || createdAt < 0L) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        Long current = data.get(CREATED_AT_KEY, PersistentDataType.LONG);
        if (current == null || current != createdAt) {
            data.set(CREATED_AT_KEY, PersistentDataType.LONG, createdAt);
            stack.setItemMeta(meta);
        }
    }

    private static String stageLabel(CopperAppleOxidationStage stage) {
        return switch (stage) {
            case FRESH -> "Fresca";
            case EXPOSED -> "Expuesta";
            case WEATHERED -> "Envejecida";
            case OXIDIZED -> "Oxidada";
        };
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0L, (millis + 999L) / 1_000L);
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private boolean rollLightning(int chancePercent) {
        return chancePercent >= 100 || (chancePercent > 0 && random.nextInt(100) < chancePercent);
    }

    private static void addEffect(Player player, PotionEffectType type, int ticks, int amplifier) {
        if (ticks <= 0) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, ticks, amplifier, false, true, true));
    }
}
