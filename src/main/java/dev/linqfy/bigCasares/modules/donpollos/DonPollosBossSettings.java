package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Objects;

/**
 * Don Pollo Boss (se spawnea solo por comando).
 *
 * @param keepDistance           en la fase 2, a cuantos bloques intenta quedarse del jugador
 * @param meteorDamage           dano de cada meteorito (a quien este cerca del impacto)
 * @param meteorCooldownSeconds  cada cuanto tira un meteorito
 * @param chickenWaveSeconds     cada cuanto spawnea gallinas WhatsApp
 * @param chickensPerWave        cuantas gallinas por tanda
 * @param maxChickens            tope de gallinas vivas por boss
 * @param chickenHealth          vida de cada gallina
 * @param chickenDamage          dano de cada gallina (el de un zombie: 3)
 * @param hardMaxChickens        techo absoluto de gallinas vivas por encuentro (no escala con la dificultad)
 * @param maxMinionsPerWorld     tope de bichos de jefe vivos entre todos los encuentros de un mismo mundo
 * @param phase3                 la FASE 3 (levitar, pozo e invocaciones)
 * @param phase4                 la FASE FINAL (rayo de warden, mega saltos y Sanders fase final)
 */
public record DonPollosBossSettings(
    double health,
    double scale,
    double meleeDamage,
    double laserDamage,
    double laserRange,
    int laserCooldownSeconds,
    DonPollosItem reward,
    double keepDistance,
    double meteorDamage,
    double meteorCooldownSeconds,
    double chickenWaveSeconds,
    int chickensPerWave,
    int maxChickens,
    double chickenHealth,
    double chickenDamage,
    int hardMaxChickens,
    DonPollosBossPhase3Settings phase3,
    DonPollosBossPhase4Settings phase4,
    int maxMinionsPerWorld
) {

    /** Techo absoluto de gallinas por defecto. */
    public static final int DEFAULT_HARD_MAX_CHICKENS = 24;
    /** Tope de bichos de jefe vivos por mundo (todos los encuentros juntos) por defecto. */
    public static final int DEFAULT_MAX_MINIONS_PER_WORLD = 80;

    /** Sin techo absoluto propio: usa el de por defecto. */
    public DonPollosBossSettings(double health, double scale, double meleeDamage, double laserDamage, double laserRange,
                                 int laserCooldownSeconds, DonPollosItem reward, double keepDistance, double meteorDamage,
                                 double meteorCooldownSeconds, double chickenWaveSeconds, int chickensPerWave,
                                 int maxChickens, double chickenHealth, double chickenDamage,
                                 DonPollosBossPhase3Settings phase3, DonPollosBossPhase4Settings phase4) {
        this(health, scale, meleeDamage, laserDamage, laserRange, laserCooldownSeconds, reward, keepDistance,
            meteorDamage, meteorCooldownSeconds, chickenWaveSeconds, chickensPerWave, maxChickens, chickenHealth,
            chickenDamage, DEFAULT_HARD_MAX_CHICKENS, phase3, phase4, DEFAULT_MAX_MINIONS_PER_WORLD);
    }

    public DonPollosBossSettings {
        Objects.requireNonNull(reward, "reward");
        Objects.requireNonNull(phase3, "phase3");
        Objects.requireNonNull(phase4, "phase4");
        if (health < 1 || scale <= 0 || scale > 4 || meleeDamage < 0 || laserDamage < 0) {
            throw new IllegalArgumentException("boss: health >= 1, 0 < scale <= 4 y danos >= 0");
        }
        if (laserRange < 4 || laserRange > 64 || laserCooldownSeconds < 2) {
            throw new IllegalArgumentException("boss: laser-range entre 4 y 64 y laser-cooldown-seconds >= 2");
        }
        if (keepDistance < 3 || keepDistance > 30 || meteorDamage < 0 || meteorCooldownSeconds < 0.5) {
            throw new IllegalArgumentException("boss: keep-distance entre 3 y 30, meteor-damage >= 0 y meteor-cooldown-seconds >= 0.5");
        }
        if (chickenWaveSeconds < 1 || chickensPerWave < 0 || maxChickens < 0 || chickenHealth < 1 || chickenDamage < 0) {
            throw new IllegalArgumentException("boss: gallinas con chicken-wave-seconds >= 1, cantidades >= 0, vida >= 1 y dano >= 0");
        }
        if (hardMaxChickens < 1) {
            throw new IllegalArgumentException("boss: hard-max-chickens >= 1");
        }
        if (maxMinionsPerWorld < 1) {
            throw new IllegalArgumentException("boss: max-minions-per-world >= 1");
        }
    }

    public static DonPollosBossSettings defaults() {
        return new DonPollosBossSettings(600, 1.8, 14, 3, 28, 7, new DonPollosItem("DIAMOND", 16),
            10, 6, 2.5, 7.5, 2, 8, 8, 3, DEFAULT_HARD_MAX_CHICKENS, DonPollosBossPhase3Settings.defaults(), DonPollosBossPhase4Settings.defaults(),
            DEFAULT_MAX_MINIONS_PER_WORLD);
    }
}
