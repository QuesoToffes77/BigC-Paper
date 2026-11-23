package dev.linqfy.bigCasares.modules.toffy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToffyCurveTest {
    private static final double EPSILON = 1e-9;

    private static List<ToffyCurve> everyCurve() {
        return List.of(
            new ToffyCurve.Linear(),
            new ToffyCurve.Exponential(3.0),
            new ToffyCurve.Exponential(-3.0),
            new ToffyCurve.Logarithmic(9.0),
            new ToffyCurve.Power(2.0),
            new ToffyCurve.Power(0.5),
            new ToffyCurve.Sigmoid(10.0, 0.5),
            new ToffyCurve.Sigmoid(6.0, 0.3),
            new ToffyCurve.Points(List.of(
                new ToffyCurve.Point(0.0, 0.0),
                new ToffyCurve.Point(0.5, 0.2),
                new ToffyCurve.Point(1.0, 1.0)))
        );
    }

    @Test
    void everyCurveMapsZeroToZeroAndOneToOne() {
        for (ToffyCurve curve : everyCurve()) {
            assertEquals(0.0, curve.apply(0.0), EPSILON, curve.toString());
            assertEquals(1.0, curve.apply(1.0), EPSILON, curve.toString());
        }
    }

    @Test
    void everyCurveIsNonDecreasingAndStaysInsideTheUnitRange() {
        for (ToffyCurve curve : everyCurve()) {
            double previous = 0.0;
            for (int step = 0; step <= 1_000; step++) {
                double value = curve.apply(step / 1_000.0);
                assertTrue(value >= previous - EPSILON, curve + " decreases at step " + step);
                assertTrue(value >= 0.0 && value <= 1.0, curve + " leaves [0,1] at step " + step);
                previous = value;
            }
        }
    }

    @Test
    void inputsOutsideTheUnitRangeAreClamped() {
        for (ToffyCurve curve : everyCurve()) {
            assertEquals(0.0, curve.apply(-5.0), EPSILON);
            assertEquals(1.0, curve.apply(7.0), EPSILON);
            assertEquals(0.0, curve.apply(Double.NaN), EPSILON);
        }
    }

    @Test
    void linearReturnsTheProgressUnchanged() {
        assertEquals(0.37, new ToffyCurve.Linear().apply(0.37), EPSILON);
    }

    @Test
    void exponentialMatchesItsFormula() {
        double expected = (Math.exp(1.5) - 1.0) / (Math.exp(3.0) - 1.0);
        assertEquals(expected, new ToffyCurve.Exponential(3.0).apply(0.5), EPSILON);
        assertTrue(new ToffyCurve.Exponential(3.0).apply(0.5) < 0.5);
        assertTrue(new ToffyCurve.Exponential(-3.0).apply(0.5) > 0.5);
    }

    @Test
    void logarithmicMatchesItsFormulaAndStartsFast() {
        double expected = Math.log(1.0 + 9.0 * 0.25) / Math.log(10.0);
        assertEquals(expected, new ToffyCurve.Logarithmic(9.0).apply(0.25), EPSILON);
        assertTrue(new ToffyCurve.Logarithmic(9.0).apply(0.25) > 0.25);
    }

    @Test
    void powerMatchesItsFormula() {
        assertEquals(0.25, new ToffyCurve.Power(2.0).apply(0.5), EPSILON);
        assertEquals(Math.sqrt(0.5), new ToffyCurve.Power(0.5).apply(0.5), EPSILON);
    }

    @Test
    void sigmoidIsHalfAtASymmetricMidpointAndSteepAroundIt() {
        ToffyCurve curve = new ToffyCurve.Sigmoid(10.0, 0.5);
        assertEquals(0.5, curve.apply(0.5), EPSILON);
        assertTrue(curve.apply(0.3) < 0.15);
        assertTrue(curve.apply(0.7) > 0.85);
    }

    @Test
    void pointsInterpolateLinearlyBetweenNeighbours() {
        ToffyCurve curve = new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0),
            new ToffyCurve.Point(0.5, 0.2),
            new ToffyCurve.Point(1.0, 1.0)));
        assertEquals(0.1, curve.apply(0.25), EPSILON);
        assertEquals(0.2, curve.apply(0.5), EPSILON);
        assertEquals(0.6, curve.apply(0.75), EPSILON);
    }

    @Test
    void typesAreReportedForEveryCurve() {
        assertEquals(ToffyCurveType.LINEAR, new ToffyCurve.Linear().type());
        assertEquals(ToffyCurveType.EXPONENTIAL, new ToffyCurve.Exponential(2.0).type());
        assertEquals(ToffyCurveType.LOGARITHMIC, new ToffyCurve.Logarithmic(2.0).type());
        assertEquals(ToffyCurveType.POWER, new ToffyCurve.Power(2.0).type());
        assertEquals(ToffyCurveType.SIGMOID, new ToffyCurve.Sigmoid(2.0, 0.5).type());
        assertEquals(ToffyCurveType.POINTS, new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0), new ToffyCurve.Point(1.0, 1.0))).type());
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Exponential(0.0));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Exponential(51.0));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Exponential(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Logarithmic(0.0));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Logarithmic(-0.5));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Power(0.0));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Sigmoid(0.0, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Sigmoid(5.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Sigmoid(5.0, 1.0));
    }

    @Test
    void strengthsTooCloseToZeroAreRejectedInsteadOfCollapsingTheCurve() {
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Sigmoid(1e-17, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Logarithmic(4.9e-324));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Exponential(-1e-9));
        assertEquals(0.5, new ToffyCurve.Sigmoid(1e-6, 0.5).apply(0.5), 1e-6);
        assertEquals(0.5, new ToffyCurve.Logarithmic(1e-6).apply(0.5), 1e-6);
    }

    @Test
    void invalidPointListsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0))));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.1, 0.0), new ToffyCurve.Point(1.0, 1.0))));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0), new ToffyCurve.Point(0.9, 1.0))));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.1), new ToffyCurve.Point(1.0, 1.0))));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0), new ToffyCurve.Point(1.0, 0.8))));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0), new ToffyCurve.Point(0.5, 0.6),
            new ToffyCurve.Point(0.5, 0.7), new ToffyCurve.Point(1.0, 1.0))));
        assertThrows(IllegalArgumentException.class, () -> new ToffyCurve.Points(List.of(
            new ToffyCurve.Point(0.0, 0.0), new ToffyCurve.Point(0.5, 0.6),
            new ToffyCurve.Point(0.7, 0.4), new ToffyCurve.Point(1.0, 1.0))));
    }
}
