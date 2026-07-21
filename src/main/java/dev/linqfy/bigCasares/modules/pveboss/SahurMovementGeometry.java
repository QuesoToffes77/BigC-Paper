package dev.linqfy.bigCasares.modules.pveboss;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

final class SahurMovementGeometry {
    private static final int STEP_UP_BLOCKS = 1;
    private static final int FLOOR_SEARCH_DEPTH = 7;

    private SahurMovementGeometry() { }

    static double anchorDirectionX(double visualDirectionX) {
        return -visualDirectionX;
    }

    static double anchorDirectionZ(double visualDirectionZ) {
        return -visualDirectionZ;
    }

    static OptionalInt findFeetY(int currentFeetY, IntPredicate solid, IntPredicate passable) {
        int highestFeetY = currentFeetY + STEP_UP_BLOCKS;
        for (int feetY = highestFeetY; feetY >= highestFeetY - FLOOR_SEARCH_DEPTH; feetY--) {
            if (solid.test(feetY - 1) && passable.test(feetY) && passable.test(feetY + 1)) {
                return OptionalInt.of(feetY);
            }
        }
        return OptionalInt.empty();
    }
}
