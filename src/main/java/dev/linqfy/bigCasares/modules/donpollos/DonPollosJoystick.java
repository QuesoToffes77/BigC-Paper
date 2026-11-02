package dev.linqfy.bigCasares.modules.donpollos;

import java.util.EnumSet;
import java.util.Set;

/**
 * Batalla de baile en Bedrock (sin Bukkit). El joystick del celular muchas veces manda dos direcciones juntas
 * (izquierda + adelante, por ejemplo) aunque el jugador quiso ir para un solo lado. Bukkit solo da las teclas, no el
 * angulo del joystick; pero en Bedrock el que mueve al jugador es el propio cliente, asi que su desplazamiento real
 * dice para donde fue el joystick. Con eso se elige la direccion que domina.
 */
final class DonPollosJoystick {

    /** Ticks que se espera a que el jugador se mueva antes de decidir. */
    static final int WAIT_TICKS = 4;
    /** Desplazamiento minimo (en bloques) para decidir sin esperar mas. */
    static final double MIN_MOVE = 0.03;

    private static final Set<DonPollosDanceMove> LONGITUDINAL = EnumSet.of(DonPollosDanceMove.FORWARD, DonPollosDanceMove.BACKWARD);
    private static final Set<DonPollosDanceMove> LATERAL = EnumSet.of(DonPollosDanceMove.LEFT, DonPollosDanceMove.RIGHT);

    private DonPollosJoystick() {
    }

    /**
     * Diagonal del joystick: exactamente una direccion adelante/atras y una izquierda/derecha, nada mas. Direcciones
     * opuestas, tres teclas o saltar/agacharse con otra cosa no son joystick: siguen siendo un error.
     */
    static boolean isDiagonal(Set<DonPollosDanceMove> pressed) {
        return pressed.size() == 2
            && pressed.stream().filter(LONGITUDINAL::contains).count() == 1
            && pressed.stream().filter(LATERAL::contains).count() == 1;
    }

    /**
     * Pasa un desplazamiento del mundo (dx, dz) a lo que ve el jugador segun su yaw de Minecraft:
     * {adelante, izquierda}. Yaw 0 mira al sur (+Z) y su izquierda es el este (+X).
     */
    static double[] local(double dx, double dz, float yaw) {
        double radians = Math.toRadians(yaw);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double forward = -dx * sin + dz * cos;
        double left = dx * cos + dz * sin;
        return new double[]{forward, left};
    }

    /**
     * De las dos direcciones de la diagonal, la que domina en el movimiento real. Si el jugador casi no se movio,
     * devuelve null (no hay como saber para donde fue).
     */
    static DonPollosDanceMove dominant(Set<DonPollosDanceMove> diagonal, double forward, double left) {
        if (forward * forward + left * left < MIN_MOVE * MIN_MOVE) {
            return null;
        }
        Set<DonPollosDanceMove> axis = Math.abs(forward) >= Math.abs(left) ? LONGITUDINAL : LATERAL;
        return diagonal.stream().filter(axis::contains).findFirst().orElse(null);
    }
}
