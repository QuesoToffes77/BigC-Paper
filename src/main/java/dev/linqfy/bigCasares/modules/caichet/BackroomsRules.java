package dev.linqfy.bigCasares.modules.caichet;

import org.bukkit.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Las cuentas de las entidades de los backrooms, sin tocar Bukkit para poder probarlas. Todo en ticks (20 por
 * segundo) y bloques; quien llama le da lo que ve (distancias, si el jugador la mira) y hace lo que dice.
 */
final class BackroomsRules {

    private BackroomsRules() {
    }

    // ------------------------------------------------------------------ director

    /**
     * Cuando sortear una aparicion para cada jugador: cada 30 s hay 30 % de chance, y despues de cada encuentro hay
     * 45 s sin entidades.
     */
    static final class Director {

        static final int ROLL_EVERY_TICKS = 20 * 30;
        static final int CHANCE_PERCENT = 30;
        static final int QUIET_TICKS = 20 * 45;

        private final Map<UUID, Long> nextRoll = new HashMap<>();
        private final Map<UUID, Long> quietUntil = new HashMap<>();

        /**
         * Si toca que aparezca una. La primera vez que ve al jugador solo agenda (no aparece apenas entra). Con
         * eligible en false (ya tiene una entidad o una figura) el turno se pierde.
         */
        boolean roll(UUID player, long now, boolean eligible, RandomGenerator random) {
            Long at = nextRoll.get(player);
            if (at == null) {
                nextRoll.put(player, now + ROLL_EVERY_TICKS);
                return false;
            }
            if (now < at) {
                return false;
            }
            nextRoll.put(player, now + ROLL_EVERY_TICKS);
            if (!eligible || now < quietUntil.getOrDefault(player, 0L)) {
                return false;
            }
            return random.nextInt(100) < CHANCE_PERCENT;
        }

        /** Termino un encuentro (como termine): 45 s sin entidades. */
        void ended(UUID player, long now) {
            quietUntil.put(player, now + QUIET_TICKS);
        }

        boolean isQuiet(UUID player, long now) {
            return now < quietUntil.getOrDefault(player, 0L);
        }

        void forget(UUID player) {
            nextRoll.remove(player);
            quietUntil.remove(player);
        }

        void clear() {
            nextRoll.clear();
            quietUntil.clear();
        }
    }

    // ------------------------------------------------------------------ la muerte

    /** Quien murio por que entidad: el mensaje de muerte lo lee un rato despues del golpe. */
    static final class PendingDeaths {

        static final int EXPIRES_TICKS = 20 * 5;

        private record Entry(BackroomsKind kind, long until) {
        }

        private final Map<UUID, Entry> pending = new HashMap<>();

        void put(UUID player, BackroomsKind kind, long now) {
            pending.put(player, new Entry(kind, now + EXPIRES_TICKS));
        }

        /** La entidad que lo mato (y se olvida), o null si no hay o ya vencio. */
        BackroomsKind take(UUID player, long now) {
            Entry entry = pending.remove(player);
            return entry == null || now > entry.until() ? null : entry.kind();
        }

        void forget(UUID player) {
            pending.remove(player);
        }

        void clear() {
            pending.clear();
        }
    }

    // ------------------------------------------------------------------ el susto

    /** Los tiempos del susto (ticks desde que empieza). */
    static final int SCARE_DEATH_AT = 12;
    static final int SCARE_END_AT = 25;

    static boolean dies(GameMode mode) {
        return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }

    // ------------------------------------------------------------------ techo

    /**
     * Cuanto achicar una figura de alto height donde hay libre bloques hasta el techo: 1 si entra, y si no
     * (libre - 0,2) / alto, sin bajar de un minimo para que nunca desaparezca.
     */
    static float fit(float free, float height) {
        if (height <= 0f) {
            return 1f;
        }
        return Math.max(MIN_FIT, Math.min(1f, (free - 0.2f) / height));
    }

    static final float MIN_FIT = 0.25f;

    // ------------------------------------------------------------------ 1. sonrisa

    /** La Sonrisa: no hay que mirarla. */
    static final class Sonrisa {

        /** Mirandola este tiempo se lanza a la cara. */
        static final int LOOK_TO_LUNGE = 60;
        /** Sin mirarla este tiempo seguido, se desvanece. */
        static final int AWAY_TO_VANISH = 100;
        static final int MAX_AGE = 20 * 60;
        static final double LUNGE_SPEED = 0.8;
        static final double SEEN_ANGLE_DEGREES = 30;

        enum Event { NONE, LUNGE, VANISH, EXPIRE }

        private int look;
        private int away;
        private int age;
        private boolean lunging;

        Event tick(boolean seen) {
            age++;
            if (lunging) {
                return Event.NONE;
            }
            if (seen) {
                look++;
                away = 0;
                if (look >= LOOK_TO_LUNGE) {
                    lunging = true;
                    return Event.LUNGE;
                }
            } else {
                away++;
                if (away >= AWAY_TO_VANISH) {
                    return Event.VANISH;
                }
            }
            return age >= MAX_AGE ? Event.EXPIRE : Event.NONE;
        }

        boolean lunging() {
            return lunging;
        }

        int look() {
            return look;
        }

        int away() {
            return away;
        }

        /** Un paso del lanzamiento: la distancia que queda despues de avanzar (0 = llego). */
        static double lungeStep(double distance) {
            return Math.max(0, distance - LUNGE_SPEED);
        }
    }

    // ------------------------------------------------------------------ 2. e666 y 4. arana

    /** Las que te siguen: la Entidad 666 (lenta, pero no para) y la Araña (rapida). */
    static final class Chase {

        enum Event { NONE, CAUGHT, GONE }

        static final Chase E666 = new Chase(0.17, 1.2, 20 * 40, 48);
        static final Chase ARANA = new Chase(0.26, 1.5, 20 * 15, 50);

        final double speed;
        final double catchDistance;
        final int maxAgeTicks;
        final double maxDistance;

        private Chase(double speed, double catchDistance, int maxAgeTicks, double maxDistance) {
            this.speed = speed;
            this.catchDistance = catchDistance;
            this.maxAgeTicks = maxAgeTicks;
            this.maxDistance = maxDistance;
        }

        /** Que pasa cuando esta a distance del jugador y lleva age ticks persiguiendo (el primero es 1). */
        Event step(double distance, int age) {
            if (distance <= catchDistance) {
                return Event.CAUGHT;
            }
            if (age >= maxAgeTicks || distance > maxDistance) {
                return Event.GONE;
            }
            return Event.NONE;
        }

        /** Cuanto avanza este tick sin pasarse de la distancia que la separa del jugador. */
        double advance(double distance) {
            return Math.max(0, Math.min(speed, distance));
        }
    }

    /** Latido de la Entidad 666: cada 1,8 s si esta a mas de 10 bloques, cada 1,1 s si esta a 10 o menos. */
    static final double HEARTBEAT_NEAR = 10;
    static final int HEARTBEAT_SLOW_TICKS = 36;
    static final int HEARTBEAT_FAST_TICKS = 22;
    static final int STEP_TICKS = 12;

    static int heartbeatEvery(double distance) {
        return distance > HEARTBEAT_NEAR ? HEARTBEAT_SLOW_TICKS : HEARTBEAT_FAST_TICKS;
    }

    static String heartbeatSound(double distance) {
        return distance > HEARTBEAT_NEAR ? CaiChetSounds.E666_HEARTBEAT_SLOW : CaiChetSounds.E666_HEARTBEAT_FAST;
    }

    // ------------------------------------------------------------------ 3. juan carlos

    /** Juan Carlos: camina hacia vos, te levanta, y hay que hacer clic para que te suelte. */
    static final class JuanCarlos {

        enum Phase { WALK, GRAB, THROWN, RELEASED, DONE }

        enum Event { NONE, GRABBED, RELEASED, THROWN, SCARE, GONE }

        static final double SPEED = 0.12;
        static final double GRAB_DISTANCE = 1.6;
        static final double MAX_DISTANCE = 40;
        static final int MAX_WALK_TICKS = 20 * 45;
        static final int GRAB_TICKS = 60;
        static final int CLICKS_TO_RELEASE = 6;
        static final int THROW_TO_SCARE_TICKS = 15;
        static final int RELEASED_FADE_TICKS = 30;
        static final double MAX_LIFT = 2.0;

        private Phase phase = Phase.WALK;
        private int age;
        private int held;
        private int clicks;
        private int waited;

        Phase phase() {
            return phase;
        }

        int clicks() {
            return clicks;
        }

        int held() {
            return held;
        }

        /** Un tick, con la distancia (en horizontal) entre los dos. */
        Event tick(double distance) {
            switch (phase) {
                case WALK -> {
                    age++;
                    if (distance <= GRAB_DISTANCE) {
                        phase = Phase.GRAB;
                        return Event.GRABBED;
                    }
                    return distance > MAX_DISTANCE || age >= MAX_WALK_TICKS ? Event.GONE : Event.NONE;
                }
                case GRAB -> {
                    held++;
                    if (held >= GRAB_TICKS) {
                        phase = Phase.THROWN;
                        return Event.THROWN;
                    }
                    return Event.NONE;
                }
                case THROWN -> {
                    waited++;
                    return waited >= THROW_TO_SCARE_TICKS ? finish(Event.SCARE) : Event.NONE;
                }
                case RELEASED -> {
                    waited++;
                    return waited >= RELEASED_FADE_TICKS ? finish(Event.GONE) : Event.NONE;
                }
                default -> {
                    return Event.NONE;
                }
            }
        }

        private Event finish(Event event) {
            phase = Phase.DONE;
            return event;
        }

        /** Un clic del jugador. Cuenta solo mientras lo tiene agarrado; true si con este lo suelta. */
        boolean click() {
            if (phase != Phase.GRAB) {
                return false;
            }
            clicks++;
            if (clicks >= CLICKS_TO_RELEASE) {
                phase = Phase.RELEASED;
                waited = 0;
                return true;
            }
            return false;
        }

        /** Cuanto sube el jugador: de 0 a 2 bloques durante el agarre, sin pasar el techo (maxLift). */
        static double lift(int held, double maxLift) {
            double wanted = MAX_LIFT * Math.min(held, GRAB_TICKS) / GRAB_TICKS;
            return Math.max(0, Math.min(wanted, maxLift));
        }
    }

    // ------------------------------------------------------------------ 5. payaso

    /** El Payaso: espia desde una esquina; hay que alejarse apenas lo ves. */
    static final class Payaso {

        enum Event { NONE, SPOTTED, WIN, LOSE, GONE }

        static final int DEADLINE_TICKS = 100;
        static final double WIN_DISTANCE = 6;
        static final double LOSE_DISTANCE = 3;
        static final int NEVER_SEEN_TICKS = 20 * 30;
        static final double SEEN_ANGLE_DEGREES = 35;

        private boolean spotted;
        private double seenAt;
        private int age;
        private int left = DEADLINE_TICKS;

        boolean spotted() {
            return spotted;
        }

        int left() {
            return left;
        }

        Event tick(boolean seen, double distance) {
            age++;
            if (!spotted) {
                if (seen) {
                    spotted = true;
                    seenAt = distance;
                    return Event.SPOTTED;
                }
                return age >= NEVER_SEEN_TICKS ? Event.GONE : Event.NONE;
            }
            left--;
            if (distance - seenAt >= WIN_DISTANCE) {
                return Event.WIN;
            }
            if (seenAt - distance >= LOSE_DISTANCE || left <= 0) {
                return Event.LOSE;
            }
            return Event.NONE;
        }
    }
}
