package dev.linqfy.bigCasares.modules.specialitems;

import java.util.List;

public record NukeRing(double radius, List<NukePoint> points) {

    public NukeRing {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("radius must be positive");
        }
        points = List.copyOf(points);
        if (points.isEmpty()) {
            throw new IllegalArgumentException("ring must contain points");
        }
    }
}
