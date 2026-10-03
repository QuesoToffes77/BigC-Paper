package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.NamespacedKey;

import java.util.List;
import java.util.ArrayList;

public enum CopperAppleOxidationStage {
    FRESH(
        "bigcasares:copper_apple",
        "\u00a76Manzana de Cobre Fresca",
        List.of("\u00a77Estado: fresca", "\u00a78Curacion, absorcion y regeneracion.")
    ),
    EXPOSED(
        "bigcasares:copper_apple_exposed",
        "\u00a7eManzana de Cobre Expuesta",
        List.of("\u00a77Estado: expuesta", "\u00a7bOtorga Velocidad I.")
    ),
    WEATHERED(
        "bigcasares:copper_apple_weathered",
        "\u00a7aManzana de Cobre Envejecida",
        List.of("\u00a77Estado: envejecida", "\u00a7bOtorga Velocidad I y Fuerza I.")
    ),
    OXIDIZED(
        "bigcasares:copper_apple_oxidized",
        "\u00a73Manzana de Cobre Oxidada",
        List.of("\u00a77Estado: oxidada al maximo", "\u00a7bEfectos maximos y descarga electrica.")
    );

    private final NamespacedKey itemModel;
    private final String displayName;
    private final List<String> lore;

    CopperAppleOxidationStage(String itemModel, String displayName, List<String> lore) {
        NamespacedKey parsedModel = NamespacedKey.fromString(itemModel);
        if (parsedModel == null) {
            throw new IllegalArgumentException("Invalid copper apple item model: " + itemModel);
        }
        this.itemModel = parsedModel;
        this.displayName = displayName;
        this.lore = List.copyOf(lore);
    }

    public NamespacedKey itemModel() {
        return itemModel;
    }

    public String displayName() {
        return displayName;
    }

    public List<String> lore() {
        CopperAppleEffectProfile profile = CopperAppleBalance.profileFor(this);
        List<String> result = new ArrayList<>();
        result.add(lore.getFirst());
        result.add("\u00a7aCuracion " + level(profile.instantHealthAmplifier()));
        addEffectLore(result, "Absorcion", profile.absorptionTicks(), profile.absorptionAmplifier());
        addEffectLore(result, "Regeneracion", profile.regenerationTicks(), profile.regenerationAmplifier());
        addEffectLore(result, "Velocidad", profile.speedTicks(), profile.speedAmplifier());
        addEffectLore(result, "Fuerza", profile.strengthTicks(), profile.strengthAmplifier());
        addEffectLore(result, "Resistencia", profile.resistanceTicks(), profile.resistanceAmplifier());
        if (profile.lightningChancePercent() > 0) {
            result.add("\u00a78Descarga visual: " + profile.lightningChancePercent() + "% (sin dano)");
        }
        return List.copyOf(result);
    }

    private static void addEffectLore(List<String> result, String name, int ticks, int amplifier) {
        if (ticks > 0) {
            result.add("\u00a7b" + name + " " + level(amplifier) + " \u00a77(" + ticks / 20 + "s)");
        }
    }

    private static String level(int amplifier) {
        return amplifier == 0 ? "I" : amplifier == 1 ? "II" : Integer.toString(amplifier + 1);
    }
}
