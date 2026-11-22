package dev.linqfy.bigCasares.modules.alexscaves.toxic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukeMathTest {
    @Test
    void sizesFollowTheMod() {
        assertEquals(15, NukeMath.radius(NukeMath.NUCLEEPER_SIZE));
        assertEquals(30, NukeMath.radius(NukeMath.CHARGED_NUCLEEPER_SIZE));
        assertEquals(135, NukeMath.radius(NukeMath.BOMB_SIZE), "three times the mod's 45");
        assertEquals(1, NukeMath.irradiatedAmplifier(NukeMath.NUCLEEPER_SIZE));
        assertEquals(2, NukeMath.irradiatedAmplifier(NukeMath.BOMB_SIZE));
    }

    @Test
    void damageFallsWithDistance() {
        assertEquals(100.0F, NukeMath.damage(1.0F, 0.0F), 1e-4);
        assertEquals(700.0F, NukeMath.damage(3.0F, 0.0F), 1e-4, "bomb: 100 + 1.5 x 400");
        assertTrue(NukeMath.damage(1.0F, 10.0F) < NukeMath.damage(1.0F, 5.0F));
        assertEquals(0.0F, NukeMath.damage(1.0F, 40.0F), "nothing beyond 1.5 x radius + 1");
    }

    @Test
    void craterIsOpenAtTheCentreAndBoundedByTheRadius() {
        int radius = NukeMath.radius(NukeMath.NUCLEEPER_SIZE);
        assertTrue(NukeMath.inCrater(0, 0, 0, 0, 0, 0, radius));
        int inside = 0;
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                for (int y = -20; y <= 20; y += 2) {
                    if (NukeMath.inCrater(x, y, z, 0, 0, 0, radius)) {
                        inside++;
                        assertTrue(x * x + z * z <= 2 * radius * radius, "within the crater width at " + x + "," + z);
                        assertTrue(Math.abs(y) <= radius, "within the crater height at " + y);
                    }
                }
            }
        }
        assertTrue(inside > 1000, "a real crater: " + inside);
        assertFalse(NukeMath.inCrater(0, 18, 0, 0, 0, 0, radius), "closed above 0.6 x radius");
    }

    @Test
    void radiationResistantMobsIncludeTheToxicOnes() {
        assertTrue(NukeMath.RESISTS_RADIATION.contains("alexscaves:nucleeper"));
        assertTrue(NukeMath.RESISTS_RADIATION.contains("alexscaves:raycat"));
        assertEquals(0.025F, NukeMath.dropChance(1.0F), 1e-6);
    }

    /** A skipped cube must not hold a single crater block (brute force over the bomb's old size and a Nucleeper). */
    @Test
    void skippedCubesHoldNoCraterBlock() {
        for (float size : new float[] {NukeMath.NUCLEEPER_SIZE, 3.0F}) {
            int radius = NukeMath.radius(size);
            int chunks = NukeMath.chunksAffected(size);
            int cx = 8, cy = 64, cz = -3;
            int skipped = 0;
            for (int i = -chunks; i <= chunks; i++) {
                for (int j = -chunks; j <= chunks; j++) {
                    for (int k = -chunks; k <= chunks; k++) {
                        if (NukeMath.cubeMayHoldCrater(i * 16, j * 16, k * 16, radius)) {
                            continue;
                        }
                        skipped++;
                        for (int x = 0; x < 16; x++) {
                            for (int y = 0; y < 16; y++) {
                                for (int z = 0; z < 16; z++) {
                                    int bx = cx + i * 16 + x, by = cy + j * 16 + y, bz = cz + k * 16 + z;
                                    assertFalse(NukeMath.inCrater(bx, by, bz, cx, cy, cz, radius),
                                        "size " + size + " cube " + i + "," + j + "," + k);
                                }
                            }
                        }
                    }
                }
            }
            assertTrue(skipped > 0, "some cubes are skipped at size " + size);
        }
    }

    @Test
    void theBombCarvesInAboutTwentySecondsAndFlingIsBounded() {
        assertEquals(3, NukeMath.cubesPerTick(27));
        assertEquals(6, NukeMath.cubesPerTick(2400));
        assertEquals(135, NukeMath.radius(NukeMath.BOMB_SIZE));
        assertTrue(NukeMath.MAX_FLING <= 4.0);
        assertEquals(120, NukeMath.BLAST_TICKS);
    }
}
