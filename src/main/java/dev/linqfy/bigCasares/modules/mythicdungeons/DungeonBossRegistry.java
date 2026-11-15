package dev.linqfy.bigCasares.modules.mythicdungeons;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Que jefe invocado vive en que mazmorra (sin servidor), para no dejar invocar un segundo mientras el primero siga
 * activo en el sistema pve-boss. Las entradas muertas se olvidan al consultarlas.
 */
public final class DungeonBossRegistry {

    /** Una invocacion: el jefe y donde aparecio. */
    public record Summon(UUID bossInstanceId, MythicCulture culture, String world, double x, double y, double z) {
    }

    private final Map<String, Summon> byDungeon = new LinkedHashMap<>();

    /** El jefe vivo de la mazmorra, si hay; olvida el que ya no esta activo. */
    public Optional<Summon> alive(String dungeonKey, Predicate<UUID> active) {
        Summon summon = byDungeon.get(dungeonKey);
        if (summon == null) {
            return Optional.empty();
        }
        if (!active.test(summon.bossInstanceId())) {
            byDungeon.remove(dungeonKey);
            return Optional.empty();
        }
        return Optional.of(summon);
    }

    public void remember(String dungeonKey, Summon summon) {
        byDungeon.put(dungeonKey, summon);
    }

    /** Todas las invocaciones recordadas (vivas o no). */
    public Map<String, Summon> snapshot() {
        return Map.copyOf(byDungeon);
    }

    public void forget(UUID bossInstanceId) {
        byDungeon.values().removeIf(summon -> summon.bossInstanceId().equals(bossInstanceId));
    }

    public void clear() {
        byDungeon.clear();
    }
}
