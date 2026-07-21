package dev.linqfy.bigCasares.modules.pveboss;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class BossDefinitionCatalog {

    private final Map<String, AbyssGuardianDefinition> definitionsById;

    public BossDefinitionCatalog(Collection<AbyssGuardianDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<String, AbyssGuardianDefinition> indexed = new LinkedHashMap<>();
        for (AbyssGuardianDefinition definition : definitions) {
            AbyssGuardianDefinition required = Objects.requireNonNull(definition, "definition");
            String id = normalize(required.id());
            if (indexed.putIfAbsent(id, required) != null) {
                throw new IllegalArgumentException("duplicate boss id: " + id);
            }
        }
        definitionsById = Map.copyOf(indexed);
    }

    public AbyssGuardianDefinition require(String id) {
        String normalized = normalize(id);
        AbyssGuardianDefinition definition = definitionsById.get(normalized);
        if (definition == null) {
            throw new IllegalArgumentException("Boss desconocido: " + normalized);
        }
        return definition;
    }

    public List<String> ids() {
        return definitionsById.keySet().stream().sorted().toList();
    }

    private static String normalize(String id) {
        String normalized = Objects.requireNonNull(id, "id").strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("boss id must not be blank");
        }
        return normalized;
    }
}
