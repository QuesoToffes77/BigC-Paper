package dev.linqfy.bigCasares.items.catalog;

import dev.linqfy.bigCasares.items.ModelDataUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.Objects;

public final class CatalogItemStackFactory {

    private final NamespacedKey itemIdKey = new NamespacedKey("bigcasares", "item_id");
    private final NamespacedKey itemRevisionKey = new NamespacedKey("bigcasares", "item_revision");
    private final NamespacedKey itemLegacyModelDataKey = new NamespacedKey("bigcasares", "legacy_model_data");
    private final NamespacedKey defaultNameKey = new NamespacedKey("bigcasares", "item_default_name");
    private final NamespacedKey defaultLoreKey = new NamespacedKey("bigcasares", "item_default_lore");

    public ItemStack create(CustomItemDefinition definition, String revision, int amount) {
        Objects.requireNonNull(definition, "definition");
        Material material = resolveMaterial(definition.material());
        ItemStack stack = new ItemStack(material, clampAmount(amount, definition.maxStackSize(), material));
        apply(stack, definition, revision, false);
        return stack;
    }

    public boolean reconcile(ItemStack stack, CustomItemDefinition definition, String revision) {
        Objects.requireNonNull(stack, "stack");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(revision, "revision");
        Material material = resolveMaterial(definition.material());
        if (stack.getType() != material) {
            return false;
        }
        apply(stack, definition, revision, true);
        return true;
    }

    private void apply(ItemStack stack, CustomItemDefinition definition, String revision, boolean preservePresentation) {
        Material material = resolveMaterial(definition.material());
        stack.setType(material);
        stack.setAmount(clampAmount(stack.getAmount(), definition.maxStackSize(), material));
        ItemMeta meta = stack.getItemMeta();
        NamespacedKey model = NamespacedKey.fromString(definition.itemModel());
        if (model == null) {
            throw new IllegalArgumentException("invalid item model: " + definition.itemModel());
        }
        meta.setItemModel(model);
        if (!preservePresentation || isDefaultName(meta)) {
            setDisplayName(meta, definition.display());
        }
        if (!preservePresentation || isDefaultLore(meta)) {
            meta.setLore(definition.display().lore());
        }
        definition.foodDefinition().ifPresent(food -> applyFood(meta, food));
        definition.combatDefinition().ifPresent(combat -> applyCombat(meta, definition.id(), combat));
        if (definition.maxDamage() != null) {
            if (!(meta instanceof Damageable damageable)) {
                throw new IllegalArgumentException(
                    "max-damage requires a damageable material: " + definition.material());
            }
            damageable.setMaxDamage(definition.maxDamage());
        }
        definition.legacyCustomModelData().ifPresent(modelData -> {
            ModelDataUtil.writeCustomModelData(meta, modelData);
            meta.getPersistentDataContainer().set(itemLegacyModelDataKey, PersistentDataType.INTEGER, modelData);
        });
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, definition.id());
        meta.getPersistentDataContainer().set(itemRevisionKey, PersistentDataType.STRING, revision);
        meta.getPersistentDataContainer().set(defaultNameKey, PersistentDataType.STRING, definition.display().fallbackName());
        meta.getPersistentDataContainer().set(defaultLoreKey, PersistentDataType.STRING,
            String.join("\u001f", definition.display().lore()));
        stack.setItemMeta(meta);
    }

    public NamespacedKey itemIdKey() {
        return itemIdKey;
    }

    public NamespacedKey itemRevisionKey() {
        return itemRevisionKey;
    }

    public NamespacedKey itemLegacyModelDataKey() {
        return itemLegacyModelDataKey;
    }

    private boolean isDefaultName(ItemMeta meta) {
        String expected = meta.getPersistentDataContainer().get(defaultNameKey, PersistentDataType.STRING);
        return expected != null && expected.equals(meta.getDisplayName());
    }

    private boolean isDefaultLore(ItemMeta meta) {
        String expected = meta.getPersistentDataContainer().get(defaultLoreKey, PersistentDataType.STRING);
        java.util.List<String> lore = meta.getLore();
        return expected != null && String.join("\u001f", lore == null ? java.util.List.of() : lore).equals(expected);
    }

    private static Material resolveMaterial(String materialName) {
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            throw new IllegalArgumentException("item material is not available: " + materialName);
        }
        return material;
    }

    private static int clampAmount(int amount, int definitionMaximum, Material material) {
        return Math.max(1, Math.min(amount, definitionMaximum));
    }

    private static void setDisplayName(ItemMeta meta, ItemDisplayDefinition display) {
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method translatable = componentClass.getMethod("translatable", String.class);
            Object component = translatable.invoke(null, display.translationKey());
            Method setDisplayName = meta.getClass().getMethod("setDisplayName", componentClass);
            setDisplayName.invoke(meta, component);
        } catch (Throwable ignored) {
            meta.setDisplayName(display.fallbackName());
        }
    }

    private static void applyFood(ItemMeta meta, ItemFoodDefinition definition) {
        FoodComponent food = meta.getFood();
        food.setNutrition(definition.nutrition());
        food.setSaturation(definition.saturation());
        food.setCanAlwaysEat(definition.canAlwaysEat());
        meta.setFood(food);
    }

    static void applyCombat(ItemMeta meta, String itemId, ItemCombatDefinition combat) {
        CombatModifierPlan plan = combatModifierPlan(itemId, combat);
        replaceCombatModifier(
            meta,
            Attribute.ATTACK_DAMAGE,
            new NamespacedKey("bigcasares", plan.damageKey()),
            plan.damageAmount()
        );
        replaceCombatModifier(
            meta,
            Attribute.ATTACK_SPEED,
            new NamespacedKey("bigcasares", plan.speedKey()),
            plan.speedAmount()
        );
    }

    static CombatModifierPlan combatModifierPlan(String itemId, ItemCombatDefinition combat) {
        java.util.Objects.requireNonNull(itemId, "itemId");
        java.util.Objects.requireNonNull(combat, "combat");
        return new CombatModifierPlan(
            itemId + "_attack_damage",
            combat.attackDamage() - 1.0,
            itemId + "_attack_speed",
            combat.attackSpeed() - 4.0
        );
    }

    record CombatModifierPlan(String damageKey, double damageAmount, String speedKey, double speedAmount) { }

    private static void replaceCombatModifier(
        ItemMeta meta,
        Attribute attribute,
        NamespacedKey key,
        double amount
    ) {
        java.util.Collection<AttributeModifier> existing = meta.getAttributeModifiers(attribute);
        if (existing != null) {
            for (AttributeModifier modifier : java.util.List.copyOf(existing)) {
                if (modifier.getKey().equals(key)) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
            }
        }
        meta.addAttributeModifier(attribute, new AttributeModifier(
            key, amount, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND
        ));
    }
}
