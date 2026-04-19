package dev.linqfy.bigCasares.modules.missions;

import java.util.List;

public final class MissionCatalog {

    private final List<MissionDefinition> definitions;

    private MissionCatalog(List<MissionDefinition> definitions) {
        this.definitions = List.copyOf(definitions);
    }

    public static MissionCatalog of(List<MissionDefinition> definitions) {
        return new MissionCatalog(definitions);
    }

    public List<MissionDefinition> byScope(MissionScope scope) {
        return definitions.stream()
            .filter(MissionDefinition::enabled)
            .filter(definition -> definition.scope() == scope)
            .toList();
    }

    public List<MissionDefinition> all() {
        return definitions;
    }
}
