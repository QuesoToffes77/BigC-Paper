package dev.linqfy.bigCasares.modules.toffy;

import dev.linqfy.bigCasares.modules.toffy.eventos.ToffyEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Help and first-argument completion of {@code /toffy}, filtered by what the sender may use. Pure: the permissions
 * come as a predicate ("has this node, or the admin node").
 */
final class ToffyHelp {
    private static final String PREFIX = "§6Toffy §8» §7";

    /** One help line; {@code anyOf} empty means everyone. */
    private record Entry(String group, String text, List<String> anyOf) {
    }

    private static final List<Entry> ENTRIES = List.of(
        new Entry("Jugadores", "§f/toffy nivel [jugador] §7- tu nivel de equipo, o el de otro jugador",
            List.of(ToffyCommand.CHECK_PERMISSION, ToffyCommand.CHECK_OTHERS_PERMISSION)),
        new Entry("Jugadores", "§f/toffy ayuda §7- esta lista de comandos", List.of()),
        new Entry("Administracion", "§f/toffy curva §7- muestra la curva de progreso -> nivel",
            List.of(ToffyCommand.ADMIN_PERMISSION)),
        new Entry("Administracion", "§f/toffy reload §7- recarga los archivos de Toffy",
            List.of(ToffyCommand.ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo crear <nombre> §7- crea un torneo",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo lista §7- lista los torneos",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo ver <nombre> §7- anotados, lugar y teletransporte",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo abrir <nombre> §7- abre las inscripciones",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo cerrar <nombre> [mensaje...] §7- cierra las inscripciones",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo lugar <torneo> [agregar|borrar] §7- guarda tu ubicacion como lugar del torneo",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo nivel <torneo> <min|*> <max|*> | sin §7- rango de nivel para anotarse",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo cupo <torneo> <n|sin> §7- cierra las inscripciones al llegar a n",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo minimo <torneo> <n|sin> §7- minimo de anotados conectados para programados",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo cierre <torneo> <HH:mm> [dd/MM] | cancelar §7- cierre programado",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy programados §7- lista teletransportes, cierres y eventos programados",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo sacar <nombre> <jugador> §7- saca a un jugador del torneo",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo mensaje <nombre> <caso> <texto...|-|reset> §7- mensaje propio del torneo",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Torneos", "§f/toffy torneo borrar <nombre> [forzar] §7- borra el torneo",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Teletransporte", "§f/toffy tp concursantes <torneo> [lugar | x y z [mundo]] §7- lleva a los anotados",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Teletransporte", "§f/toffy tp devolver <torneo> §7- devuelve a los anotados a donde estaban",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Teletransporte", "§f/toffy tp programar <torneo> <HH:mm> [dd/MM] §7- programa la ida al lugar",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Teletransporte", "§f/toffy tp programar <torneo> cancelar §7- cancela la ida programada",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Teletransporte", "§f/toffy tp programados §7- lista las idas programadas",
            List.of(ToffyCommand.TORNEO_ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/toffy evento iniciar <tipo> <torneo> [segundos] §7- inicia un evento",
            List.of(ToffyEvents.ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/toffy evento detener §7- detiene el evento",
            List.of(ToffyEvents.ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/toffy evento tipos §7- lista los tipos de evento",
            List.of(ToffyEvents.ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/toffy evento estado §7- muestra el evento en curso",
            List.of(ToffyEvents.ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/toffy evento programar <tipo> <torneo> <HH:mm> [dd/MM] [segundos] §7- programa un evento",
            List.of(ToffyEvents.ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/toffy evento programar <torneo> cancelar §7- cancela el evento programado",
            List.of(ToffyEvents.ADMIN_PERMISSION)),
        new Entry("Eventos", "§f/evento unirse §7- te anota al torneo del evento en espera y entras",
            List.of(ToffyEventoCommand.JOIN_PERMISSION, ToffyAnotarCommand.PERMISSION)),
        new Entry("Eventos", "§f/evento salir §7- sales del evento en curso",
            List.of(ToffyEventoCommand.JOIN_PERMISSION, ToffyAnotarCommand.PERMISSION)));

    private ToffyHelp() {
    }

    /**
     * The help lines the permissions allow, grouped. Never lists a command the predicate refuses.
     */
    static List<String> lines(Predicate<String> has) {
        List<String> lines = new ArrayList<>();
        lines.add(PREFIX + "Comandos de /toffy que podes usar:");
        String group = null;
        for (Entry entry : ENTRIES) {
            if (!allowedBy(entry.anyOf(), has)) {
                continue;
            }
            if (!entry.group().equals(group)) {
                group = entry.group();
                lines.add("§e" + group + ":");
            }
            lines.add("§7  " + entry.text());
        }
        return List.copyOf(lines);
    }

    /**
     * First-argument options of {@code /toffy} for the permissions. The online player names are added by the caller.
     */
    static List<String> firstArgs(Predicate<String> has) {
        List<String> options = new ArrayList<>(List.of("ayuda"));
        if (has.test(ToffyCommand.CHECK_PERMISSION) || has.test(ToffyCommand.CHECK_OTHERS_PERMISSION)) {
            options.add("nivel");
        }
        if (has.test(ToffyCommand.ADMIN_PERMISSION)) {
            options.add("curva");
            options.add("reload");
        }
        if (has.test(ToffyCommand.TORNEO_ADMIN_PERMISSION)) {
            options.add("torneo");
            options.add("tp");
            options.add("programados");
        }
        if (has.test(ToffyEvents.ADMIN_PERMISSION)) {
            options.add("evento");
        }
        return List.copyOf(options);
    }

    private static boolean allowedBy(List<String> anyOf, Predicate<String> has) {
        return anyOf.isEmpty() || anyOf.stream().anyMatch(has);
    }
}
