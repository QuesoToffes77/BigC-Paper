package dev.linqfy.bigCasares.modules.mythicdungeons;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Las nueve culturas de las mazmorras miticas (sin Bukkit). Todas tienen su mazmorra (la estructura
 * {@code bigc_myth:<id>} del datapack). Solo las {@link #developed() desarrolladas} tienen ademas jefe (del sistema
 * pve-boss), mobs, Fragmento de Odio e Invocador; en las mazmorras de las demas todo sigue vanilla por ahora.
 * Los ids son estables: los usan los items, los modelos, el datapack y los encuentros de los jefes.
 */
public enum MythicCulture {

    GREEK("greek", "Griega", "la mazmorra griega",
        new MythicBoss("myth-zeus", "zeus", "Zeus"), List.of("minotaur", "cerberus", "erinys")),
    NORSE("norse", "Nórdica", "la mazmorra nórdica",
        new MythicBoss("myth-odin", "odin", "Odín"), List.of("draugr", "dwarf", "nidhogg", "hel")),
    MEDIEVAL("medieval", "Europa medieval", "la mazmorra medieval",
        new MythicBoss("myth-diablo", "diablo", "Diablo"), List.of("treasure_dragon", "kobold", "goblin", "ghost")),
    EGYPTIAN("egyptian", "Egipcia", "la mazmorra egipcia",
        new MythicBoss("myth-horus", "horus", "Horus"), List.of("ammit", "apophis", "mummy")),
    MESOPOTAMIAN("mesopotamian", "Mesopotámica", "la mazmorra mesopotámica",
        new MythicBoss("myth-enlil", "enlil", "Enlil"), List.of("ereshkigal", "galla")),
    // Solo mazmorra por ahora: jefe, mobs e items quedan para mas adelante.
    JAPANESE("japanese", "Japonesa", "la mazmorra japonesa", null, List.of()),
    MESOAMERICAN("mesoamerican", "Mesoamericana", "la mazmorra mesoamericana", null, List.of()),
    ARABIAN("arabian", "Árabe", "la mazmorra árabe", null, List.of()),
    ANDEAN("andean", "Sudamericana", "la mazmorra sudamericana", null, List.of());

    /** Espacio de nombres de las estructuras del datapack de las mazmorras. */
    public static final String STRUCTURE_NAMESPACE = "bigc_myth";
    /** Prefijo de todas las claves de modelo de BetterModel del modulo. */
    public static final String MODEL_PREFIX = "bigcasares_myth_";

    /** El jefe de una cultura desarrollada: su id en pve-boss ({@code bosses/<id>.yml}), id corto y nombre. */
    public record MythicBoss(String id, String shortId, String displayName) {

        public MythicBoss {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(shortId, "shortId");
            Objects.requireNonNull(displayName, "displayName");
        }

        /** Clave de BetterModel: {@code bigcasares_myth_<id corto>}. */
        public String modelKey() {
            return MODEL_PREFIX + shortId;
        }
    }

    private final String id;
    private final String displayName;
    private final String dungeonName;
    private final MythicBoss boss;
    private final List<String> mobIds;

    MythicCulture(String id, String displayName, String dungeonName, MythicBoss boss, List<String> mobIds) {
        this.id = id;
        this.displayName = displayName;
        this.dungeonName = dungeonName;
        this.boss = boss;
        this.mobIds = List.copyOf(mobIds);
    }

    public String id() {
        return id;
    }

    /** Nombre de la cultura para mostrar ("Griega", "Europa medieval"...). */
    public String displayName() {
        return displayName;
    }

    /** Como se nombra su mazmorra en una frase ("la mazmorra griega"). */
    public String dungeonName() {
        return dungeonName;
    }

    /** Si la cultura ya tiene jefe, mobs e items (las demas solo tienen la mazmorra). */
    public boolean developed() {
        return boss != null;
    }

    public Optional<MythicBoss> boss() {
        return Optional.ofNullable(boss);
    }

    /** El jefe de una cultura desarrollada; falla con las que todavia no lo tienen. */
    public MythicBoss requireBoss() {
        if (boss == null) {
            throw new IllegalStateException("La cultura " + id + " todavía no tiene jefe");
        }
        return boss;
    }

    /** Los mobs de la cultura (vacio si no esta desarrollada). */
    public List<String> mobIds() {
        return mobIds;
    }

    public Optional<String> fragmentItemId() {
        return developed() ? Optional.of("hate_fragment_" + id) : Optional.empty();
    }

    public Optional<String> summonerItemId() {
        return developed() ? Optional.of("boss_summoner_" + id) : Optional.empty();
    }

    /** Clave de la estructura del datapack, por ejemplo {@code bigc_myth:greek}. */
    public String structureKey() {
        return STRUCTURE_NAMESPACE + ":" + id;
    }

    /** Lo que se le contesta a quien pide algo de una cultura sin desarrollar. */
    public String notAvailableMessage() {
        return "§eLa cultura " + displayName + " todavía no está disponible: por ahora solo existe su mazmorra.";
    }

    public static String mobModelKey(String mobId) {
        return MODEL_PREFIX + mobId;
    }

    public static List<MythicCulture> developedCultures() {
        return Arrays.stream(values()).filter(MythicCulture::developed).toList();
    }

    public static Optional<MythicCulture> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        for (MythicCulture culture : values()) {
            if (culture.id.equals(normalized)) {
                return Optional.of(culture);
            }
        }
        return Optional.empty();
    }

    public static Optional<MythicCulture> byStructureKey(String key) {
        if (key == null || !key.startsWith(STRUCTURE_NAMESPACE + ":")) {
            return Optional.empty();
        }
        return byId(key.substring(STRUCTURE_NAMESPACE.length() + 1));
    }

    public static Optional<MythicCulture> byBossId(String bossId) {
        for (MythicCulture culture : values()) {
            if (culture.boss != null && culture.boss.id().equals(bossId)) {
                return Optional.of(culture);
            }
        }
        return Optional.empty();
    }

    /** La cultura cuyo Fragmento de Odio tiene este id de item, si es uno. */
    public static Optional<MythicCulture> byFragmentItemId(String itemId) {
        for (MythicCulture culture : values()) {
            if (itemId != null && culture.fragmentItemId().filter(itemId::equals).isPresent()) {
                return Optional.of(culture);
            }
        }
        return Optional.empty();
    }

    /** La cultura cuyo Invocador tiene este id de item, si es uno. */
    public static Optional<MythicCulture> bySummonerItemId(String itemId) {
        for (MythicCulture culture : values()) {
            if (itemId != null && culture.summonerItemId().filter(itemId::equals).isPresent()) {
                return Optional.of(culture);
            }
        }
        return Optional.empty();
    }
}
