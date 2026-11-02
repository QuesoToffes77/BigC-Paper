package dev.linqfy.bigCasares.modules.donpollos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

/**
 * Donde se cava el cubo de un jugador (sin Bukkit): en su chunk si se puede; si no (zona protegida o el cubo de otro
 * boss), en el chunk permitido mas cercano, a {@link #MAX_SEARCH_CHUNKS} chunks como mucho.
 */
final class DonPollosPitPlacement {

    static final int MAX_SEARCH_CHUNKS = 4;

    private DonPollosPitPlacement() {
    }

    /** El chunk permitido mas cercano a (chunkX, chunkZ), empezando por el mismo; vacio si no hay ninguno cerca. */
    static Optional<int[]> nearestAllowed(int chunkX, int chunkZ, int radius, BiPredicate<Integer, Integer> allowed) {
        for (int[] candidate : byDistance(chunkX, chunkZ, radius)) {
            if (allowed.test(candidate[0], candidate[1])) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Los chunks hasta {@code radius} de distancia, el mismo primero y despues del mas cercano al mas lejano. */
    static List<int[]> byDistance(int chunkX, int chunkZ, int radius) {
        List<int[]> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                candidates.add(new int[]{dx, dz});
            }
        }
        // mas cerca primero; a igual distancia, un orden fijo (asi siempre elige el mismo)
        candidates.sort(Comparator.<int[]>comparingInt(c -> c[0] * c[0] + c[1] * c[1])
            .thenComparingInt(c -> c[0]).thenComparingInt(c -> c[1]));
        List<int[]> chunks = new ArrayList<>(candidates.size());
        for (int[] offset : candidates) {
            chunks.add(new int[]{chunkX + offset[0], chunkZ + offset[1]});
        }
        return chunks;
    }
}
