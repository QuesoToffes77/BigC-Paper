package dev.linqfy.bigCasares.modules.donpollos;

/**
 * Que tan fuerte sale el Don Pollo Boss (sin Bukkit). level = cuantas veces ya le gano el jugador que lo invoco.
 * Cada victoria suma un 50%: pega mas, ataca mas seguido, invoca mas bichos y los bichos son mas rapidos y pegan mas.
 */
record DonPollosBossDifficulty(int level) {

    /** Cuanto mas fuerte por cada victoria anterior. */
    static final double STEP = 0.5;
    /** La velocidad no pasa del doble: mas rapido los bichos ya no pueden caminar bien. */
    static final double MAX_SPEED = 2.0;
    /** Un ataque nunca se repite mas seguido que una vez por segundo. */
    static final long MIN_COOLDOWN_TICKS = 20;

    DonPollosBossDifficulty {
        level = Math.max(0, level);
    }

    double factor() {
        return 1 + STEP * level;
    }

    double damage(double base) {
        return base * factor();
    }

    /** Espera entre ataques (en ticks): se divide por el factor, sin bajar de un segundo (ni subir si ya era menos). */
    long cooldownTicks(double baseTicks) {
        long scaled = Math.round(baseTicks / factor());
        return Math.max(Math.min(MIN_COOLDOWN_TICKS, Math.round(baseTicks)), scaled);
    }

    int count(int base) {
        return (int) Math.ceil(base * factor());
    }

    double speed(double base) {
        return base * Math.min(MAX_SPEED, factor());
    }

    /** Ronda de las Batallas del Cubo con mas Labubus y que salen mas seguido. */
    DonPollosCubeRound round(DonPollosCubeRound round) {
        return new DonPollosCubeRound(count(round.labubus()), Math.max(0.05, round.spawnSeconds() / factor()),
            round.colonelSpeed(), round.colonelDamage());
    }

    /** Lo que se le dice al que lo invoca. */
    static String summonMessage(int wins) {
        if (wins <= 0) {
            return "§4Todavia no le ganaste nunca a Don Pollo.";
        }
        int stronger = (int) Math.round(STEP * wins * 100);
        return "§4Le ganaste §f" + wins + "§4 " + (wins == 1 ? "vez" : "veces") + " a Don Pollo: §cesta vez es "
            + stronger + "% mas fuerte.";
    }
}
