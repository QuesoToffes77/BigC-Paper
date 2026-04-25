package dev.linqfy.bigCasares.modules.customcrossbow;

public record CustomCrossbowLoadResult(
    CustomCrossbowChargeType chargeType,
    int fireworkPower,
    int chargeCount,
    int remainingOffhandAmount
) {
}
