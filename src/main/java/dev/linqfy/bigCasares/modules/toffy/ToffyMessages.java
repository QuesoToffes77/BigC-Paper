package dev.linqfy.bigCasares.modules.toffy;

import dev.linqfy.bigCasares.modules.toffy.eventos.ToffyEvents;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat messages the library sends for each case. Pure: no Bukkit. A template comes from the tournament override,
 * then from {@code toffy_mensajes.yml}, then from the built-in default. An empty template means "do not send".
 */
final class ToffyMessages {
    static final String ANOTADO = "anotado";
    static final String YA_ANOTADO = "ya-anotado";
    static final String INSCRIPCION_CERRADA = "inscripcion-cerrada";
    static final String TORNEO_NO_EXISTE = "torneo-no-existe";
    static final String ANUNCIO_ANOTADO = "anuncio-anotado";
    static final String CIERRE = "cierre";
    static final String APERTURA = "apertura";
    static final String LLEVADO = "llevado";
    static final String DEVUELTO = "devuelto";
    static final String SIN_LUGAR_GUARDADO = "sin-lugar-guardado";
    static final String TP_PROGRAMADO = "tp-programado";
    static final String TP_AVISO = "tp-aviso";
    static final String TP_CANCELADO = "tp-cancelado";
    static final String NIVEL_FUERA_DE_RANGO = "nivel-fuera-de-rango";
    static final String MINIMO_NO_ALCANZADO = "minimo-no-alcanzado";
    static final String EVENTO_AVISO = "evento-aviso";
    // Staff events announcements (see ToffyEvents).
    static final String EVENTO_LOBBY = ToffyEvents.CASE_LOBBY;
    static final String EVENTO_UNIDO = ToffyEvents.CASE_JOINED;
    static final String EVENTO_SALIO = ToffyEvents.CASE_LEFT;
    static final String EVENTO_EMPIEZA = ToffyEvents.CASE_STARTED;
    static final String EVENTO_GANADOR = ToffyEvents.CASE_WINNER;
    static final String EVENTO_ELIMINADO = ToffyEvents.CASE_ELIMINATED;
    static final String EVENTO_CANCELADO = ToffyEvents.CASE_CANCELLED;
    static final String EVENTO_FIN = ToffyEvents.CASE_FINISHED;

    private static final Map<String, String> DEFAULTS = defaults();
    static final List<String> CASES = List.copyOf(DEFAULTS.keySet());

    record Loaded(ToffyMessages messages, List<String> warnings) {
    }

    private final Map<String, String> fileValues;

    private ToffyMessages(Map<String, String> fileValues) {
        this.fileValues = Map.copyOf(fileValues);
    }

    private static Map<String, String> defaults() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(ANOTADO, "&aTe anotaste al torneo &e{torneo}&a. Sos el participante &e#{cantidad}&a.");
        map.put(YA_ANOTADO, "&eYa estabas anotado en el torneo &6{torneo}&e.");
        map.put(INSCRIPCION_CERRADA, "&cLa inscripcion al torneo &e{torneo}&c esta cerrada.");
        map.put(TORNEO_NO_EXISTE, "&cNo existe el torneo &e{torneo}&c.");
        map.put(ANUNCIO_ANOTADO, "");
        map.put(CIERRE, "&6Se cerraron las inscripciones al torneo &e{torneo}&6. Participantes: &e{cantidad}&6.");
        map.put(APERTURA, "");
        map.put(LLEVADO, "&aFuiste llevado al torneo &e{torneo}&a.");
        map.put(DEVUELTO, "&aVolviste a donde estabas antes del torneo &e{torneo}&a.");
        map.put(SIN_LUGAR_GUARDADO, "&cNo hay un lugar guardado para devolverte del torneo &e{torneo}&c.");
        map.put(TP_PROGRAMADO, "&6Teletransporte del torneo &e{torneo}&6 programado para el &e{fecha}&6 a las &e{hora}&6.");
        map.put(TP_AVISO, "&6El torneo &e{torneo}&6 se teletransporta en &e{minutos} min&6.");
        map.put(TP_CANCELADO, "&7Se cancelo el teletransporte programado del torneo &e{torneo}&7.");
        map.put(NIVEL_FUERA_DE_RANGO, "&cNo podes entrar al torneo &e{torneo}&c: tu nivel es &e{nivel}&c y el torneo pide entre &e{min}&c y &e{max}&c.");
        map.put(MINIMO_NO_ALCANZADO, "&cSe cancelo el programado del torneo &e{torneo}&c: hay &e{cantidad}&c anotados conectados y el minimo es &e{minimo}&c.");
        map.put(EVENTO_AVISO, "&6El evento &e{evento}&6 del torneo &e{torneo}&6 empieza en &e{minutos} min&6.");
        map.put(EVENTO_LOBBY, "&6Va a empezar el evento &e{evento}&6 en &e{segundos} s&6. Anotate con &e/anotar {torneo}&6 para entrar.");
        map.put(EVENTO_UNIDO, "&aEntraste al evento &e{evento}&a. Tu inventario queda guardado y lo recuperas al salir.");
        map.put(EVENTO_SALIO, "&7{jugador} salio del evento &e{evento}&7. Quedan &e{cantidad}&7.");
        map.put(EVENTO_EMPIEZA, "&e{evento} empieza! &7Participantes: &e{cantidad}&7.");
        map.put(EVENTO_GANADOR, "&6&l{jugador} &7gano el evento &e{evento}&7.");
        map.put(EVENTO_ELIMINADO, "&c{jugador} quedo eliminado ({motivo}). &7Quedan &e{cantidad}&7.");
        map.put(EVENTO_CANCELADO, "&c{evento} se cancelo. &7{motivo}");
        map.put(EVENTO_FIN, "&e{evento} &7termino. {detalle}");
        return map;
    }

    static ToffyMessages builtIn() {
        return new ToffyMessages(Map.of());
    }

    static boolean isCase(String key) {
        return DEFAULTS.containsKey(key);
    }

    static String defaultTemplate(String caseKey) {
        return DEFAULTS.getOrDefault(caseKey, "");
    }

    /**
     * Reads the file values. Unknown keys and values that are not text only produce warnings; those cases keep
     * their built-in default.
     */
    static Loaded fromValues(Map<String, ?> raw) {
        List<String> warnings = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                if (!isCase(key)) {
                    warnings.add("toffy_mensajes.yml: caso desconocido '" + key + "' (se ignora). Casos validos: "
                        + String.join(", ", CASES));
                } else if (value == null) {
                    values.put(key, "");
                } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                    values.put(key, String.valueOf(value));
                } else {
                    warnings.add("toffy_mensajes.yml: '" + key + "' debe ser un texto; se usa el valor por defecto.");
                }
            });
        }
        return new Loaded(new ToffyMessages(values), List.copyOf(warnings));
    }

    /**
     * The template for a case before substituting placeholders: override, file or default.
     */
    String template(String caseKey, String override) {
        if (override != null) {
            return override;
        }
        String fromFile = fileValues.get(caseKey);
        return fromFile != null ? fromFile : defaultTemplate(caseKey);
    }

    /**
     * Final text with colors and placeholders applied, or {@code null} when the template is empty.
     */
    String render(String caseKey, String override, Map<String, String> placeholders) {
        return format(template(caseKey, override), placeholders);
    }

    /**
     * Applies {@code &} color codes and replaces {@code {name}} placeholders. Returns {@code null} for an empty
     * template. Colors are translated first so a value containing {@code &} is shown as typed.
     */
    static String format(String template, Map<String, String> placeholders) {
        if (template == null || template.isBlank()) {
            return null;
        }
        String text = colors(template);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return text;
    }

    static String colors(String text) {
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(chars[i + 1]) >= 0) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
