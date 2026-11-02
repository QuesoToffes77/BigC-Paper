package dev.linqfy.bigCasares.modules.donpollos;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** A quien le pega el laser de los ojos (sin Bukkit). */
final class DonPollosLaser {

    private DonPollosLaser() {
    }

    /**
     * Jugadores tocados por los rayos de cada ojo (null si ese rayo no toco a nadie). Recibe el golpe quien el rayo
     * toco de verdad, no el objetivo de atras; si los dos ojos le pegan al mismo, cuenta una sola vez.
     */
    static <T> Set<T> victims(Collection<T> hitsPerEye) {
        Set<T> victims = new LinkedHashSet<>();
        hitsPerEye.stream().filter(Objects::nonNull).forEach(victims::add);
        return victims;
    }
}
