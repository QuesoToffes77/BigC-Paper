package dev.linqfy.bigCasares.modules.donpollos;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Orden de un tick de la batalla de baile (sin Bukkit): primero se resuelve el joystick pendiente, despues corre el reloj. */
final class DonPollosDanceTick {

    private DonPollosDanceTick() {
    }

    /** Cuando se decide una diagonal Bedrock pendiente: se movio lo suficiente, espero WAIT_TICKS o queda 1 tick. */
    static boolean shouldResolve(double dx, double dz, long now, long joystickAt, long ticksLeft) {
        return dx * dx + dz * dz >= DonPollosJoystick.MIN_MOVE * DonPollosJoystick.MIN_MOVE
            || now - joystickAt >= DonPollosJoystick.WAIT_TICKS
            || ticksLeft <= 1;
    }

    /** Cuenta la diagonal como respuesta con el paso que domina el movimiento real (forward, left locales). */
    static DonPollosDanceBattle.Outcome resolve(DonPollosDanceBattle battle, Set<DonPollosDanceMove> diagonal,
                                                double forward, double left, long now) {
        DonPollosDanceMove move = DonPollosJoystick.dominant(diagonal, forward, left);
        return battle.input(now, move == null ? diagonal : EnumSet.of(move));
    }

    /**
     * Un tick: si hay una diagonal por resolver (diagonal != null) se cuenta primero como respuesta y despues avanza
     * el reloj. Devuelve los resultados en orden, para que el manager los muestre uno por uno.
     */
    static List<DonPollosDanceBattle.Outcome> advance(DonPollosDanceBattle battle, Set<DonPollosDanceMove> diagonal,
                                                      double forward, double left, long now) {
        List<DonPollosDanceBattle.Outcome> outcomes = new ArrayList<>(2);
        if (diagonal != null) {
            outcomes.add(resolve(battle, diagonal, forward, left, now));
        }
        outcomes.add(battle.tick(now));
        return outcomes;
    }
}
