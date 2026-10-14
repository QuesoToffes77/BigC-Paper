package dev.linqfy.bigCasares.modules.geyser;

import dev.linqfy.bigCasares.modules.celular.CelularModule;

import java.util.List;

/** Geyser custom items of the phone: one Bedrock item per Java item model, all on top of a clock. */
final class GeyserCelularItems {

    static final String JAVA_BASE_ITEM = "minecraft:clock";

    private GeyserCelularItems() {
    }

    static List<GeyserCustomItemDefinition> definitions(List<String> itemModels) {
        return itemModels.stream()
            .map(model -> new GeyserCustomItemDefinition(
                JAVA_BASE_ITEM, model, model, CelularModule.BEDROCK_ICON, CelularModule.DISPLAY_NAME))
            .toList();
    }
}
