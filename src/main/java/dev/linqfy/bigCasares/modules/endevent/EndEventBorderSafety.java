package dev.linqfy.bigCasares.modules.endevent;

public final class EndEventBorderSafety {

    private EndEventBorderSafety() {
    }

    public static EndEventBorderPoint projectInside(
        double centerX,
        double centerZ,
        double size,
        double margin,
        double x,
        double z
    ) {
        double usableHalf = Math.max(0.0, size / 2.0 - Math.max(0.0, margin));
        if (usableHalf == 0.0) {
            return new EndEventBorderPoint(centerX, centerZ);
        }
        return new EndEventBorderPoint(
            Math.max(centerX - usableHalf, Math.min(centerX + usableHalf, x)),
            Math.max(centerZ - usableHalf, Math.min(centerZ + usableHalf, z))
        );
    }

    public static boolean isInsideInset(
        double centerX,
        double centerZ,
        double size,
        double margin,
        double x,
        double z
    ) {
        EndEventBorderPoint projected = projectInside(centerX, centerZ, size, margin, x, z);
        return Double.compare(projected.x(), x) == 0 && Double.compare(projected.z(), z) == 0;
    }
}
