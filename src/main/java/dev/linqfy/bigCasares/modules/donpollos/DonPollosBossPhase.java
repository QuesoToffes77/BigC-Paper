package dev.linqfy.bigCasares.modules.donpollos;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Una fase de la pelea contra el Don Pollo Boss. Empieza cuando la vida del boss baja a
 * {@code startsAt} (fraccion de 0 a 1; la fase 1 empieza en 1.0).
 *
 * @param ai        como se mueve: {@link #BABY_ZOMBIE} (te persigue) o {@link #KEEP_AWAY} (se aleja y te mantiene a distancia)
 * @param abilities lo que hace en esta fase
 * @param message   texto grande que aparece al empezar la fase (vacio: solo el titulo de la fase)
 */
public record DonPollosBossPhase(int number, String title, double startsAt, String ai, Set<Ability> abilities,
                                 String message) {

    public static final String BABY_ZOMBIE = "baby-zombie";
    public static final String KEEP_AWAY = "keep-away";

    public enum Ability {
        /** Rayos laser de los ojos que queman. */
        LASER,
        /** Meteoritos del logo de WhatsApp. */
        METEORS,
        /** Gallinas WhatsApp. */
        CHICKENS,
        /** Al empezar se eleva y vacia el chunk de cada jugador. */
        LEVITATE,
        /** Batallas del Cubo (Labubus y el Coronel) en el pozo. */
        CUBE_BATTLE,
        /** Se agranda un poco. */
        GROW,
        /** Se infla y desinfla 2 veces y tira un rayo de warden a donde estabas. */
        WARDEN_BEAM,
        /** Se mueve a los saltos con el auto. */
        HOP,
        /** Mega salto que cae cerca tuyo con onda expansiva. */
        MEGA_JUMP,
        /** Laseres violetas finos de adorno en los ojos. */
        EYE_LASERS,
        /** Invoca Sanders fase final. */
        FINAL_COLONELS
    }

    public DonPollosBossPhase {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(ai, "ai");
        abilities = abilities == null || abilities.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(abilities));
        message = message == null ? "" : message;
        if (number < 1 || startsAt <= 0 || startsAt > 1) {
            throw new IllegalArgumentException("fase invalida: numero >= 1 y starts-at entre 0 y 1");
        }
        if (!ai.equals(BABY_ZOMBIE) && !ai.equals(KEEP_AWAY)) {
            throw new IllegalArgumentException("fase invalida: ia desconocida " + ai);
        }
    }

    public boolean has(Ability ability) {
        return abilities.contains(ability);
    }

    public boolean laser() {
        return has(Ability.LASER);
    }

    public boolean meteors() {
        return has(Ability.METEORS);
    }

    public boolean chickens() {
        return has(Ability.CHICKENS);
    }

    public boolean summons() {
        return has(Ability.CUBE_BATTLE);
    }

    public boolean levitate() {
        return has(Ability.LEVITATE);
    }

    /** Fase que corresponde a la vida actual (la ultima cuyo inicio ya se alcanzo). */
    public static DonPollosBossPhase forHealth(List<DonPollosBossPhase> phases, double healthFraction) {
        if (phases.isEmpty()) {
            throw new IllegalArgumentException("el boss necesita al menos una fase");
        }
        DonPollosBossPhase current = phases.getFirst();
        for (DonPollosBossPhase phase : phases) {
            if (healthFraction <= phase.startsAt()) {
                current = phase;
            }
        }
        return current;
    }

    /** Cuanto por debajo del umbral queda la vida al frenar un golpe (para que cuente como fase alcanzada). */
    static final double PHASE_EPSILON = 1e-3;

    /**
     * Dano que se deja pasar: una fase no se puede saltear, asi que un golpe nunca baja la vida mas alla del
     * umbral de la fase que sigue (un golpe por fase). Desde la ultima fase no hay tope: ahi se puede derrotar.
     */
    public static double allowedDamage(List<DonPollosBossPhase> phases, double maxHealth, double health, double damage) {
        return allowedDamage(phases, forHealth(phases, health / maxHealth), maxHealth, health, damage);
    }

    /** La fase activa solo avanza cuando el coordinador ejecuta su transicion, no con cada golpe del mismo tick. */
    public static double allowedDamage(List<DonPollosBossPhase> phases, DonPollosBossPhase activePhase,
                                       double maxHealth, double health, double damage) {
        if (activePhase == null) {
            return 0;
        }
        DonPollosBossPhase next = nextPhase(phases, activePhase);
        if (next == null) {
            return damage;
        }
        double landing = next.startsAt() * maxHealth - PHASE_EPSILON;
        return Math.min(damage, Math.max(0, health - landing));
    }

    /**
     * Si un golpe frenado en el umbral de la fase siguiente dejo la vida mas abajo (armadura, absorcion), a que vida
     * volverla: justo debajo de ese umbral. NaN: no hace falta corregir.
     */
    public static double overshootFix(List<DonPollosBossPhase> phases, double maxHealth, double healthBefore,
                                      double healthAfter) {
        return overshootFix(phases, forHealth(phases, healthBefore / maxHealth), maxHealth, healthBefore, healthAfter);
    }

    public static double overshootFix(List<DonPollosBossPhase> phases, DonPollosBossPhase activePhase,
                                      double maxHealth, double healthBefore, double healthAfter) {
        if (activePhase == null) {
            return Double.NaN;
        }
        DonPollosBossPhase next = nextPhase(phases, activePhase);
        if (next == null) {
            return Double.NaN;
        }
        double landing = next.startsAt() * maxHealth - PHASE_EPSILON;
        return healthAfter < landing - 1e-6 && healthAfter > 0 ? landing : Double.NaN;
    }

    private static DonPollosBossPhase nextPhase(List<DonPollosBossPhase> phases, DonPollosBossPhase current) {
        DonPollosBossPhase next = null;
        for (DonPollosBossPhase phase : phases) {
            if (phase.startsAt() < current.startsAt() && (next == null || phase.startsAt() > next.startsAt())) {
                next = phase;
            }
        }
        return next;
    }

    /**
     * FASE 1: te persigue como un bebe zombie y tira laser. FASE 2 (75%): se aleja y ataca de lejos con
     * meteoritos y gallinas. FASE 3 (50%): se eleva, te hunde el piso y arranca las Batallas del Cubo.
     * FASE FINAL (25%): se agranda, te persigue a los saltos, tira el rayo de warden, mega saltos con onda
     * expansiva, laseres violetas en los ojos e invoca Sanders fase final.
     */
    public static List<DonPollosBossPhase> defaults() {
        return List.of(
            new DonPollosBossPhase(1, "FASE 1", 1.0, BABY_ZOMBIE, EnumSet.of(Ability.LASER),
                "Un video más mi gente?"),
            new DonPollosBossPhase(2, "FASE 2", 0.75, KEEP_AWAY, EnumSet.of(Ability.METEORS, Ability.CHICKENS),
                "No hay salsa para ti..."),
            new DonPollosBossPhase(3, "FASE 3", 0.5, KEEP_AWAY, EnumSet.of(Ability.LEVITATE, Ability.CUBE_BATTLE),
                "No hay picante para ti..."),
            new DonPollosBossPhase(4, "FASE FINAL", 0.25, BABY_ZOMBIE, EnumSet.of(Ability.GROW, Ability.WARDEN_BEAM,
                Ability.HOP, Ability.MEGA_JUMP, Ability.EYE_LASERS, Ability.FINAL_COLONELS),
                "Me haz hecho enojar, Papi Don Pollo esta enojado"));
    }
}
