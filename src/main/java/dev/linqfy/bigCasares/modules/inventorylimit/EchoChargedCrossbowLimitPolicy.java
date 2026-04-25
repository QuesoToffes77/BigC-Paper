package dev.linqfy.bigCasares.modules.inventorylimit;

public record EchoChargedCrossbowLimitPolicy(int max) {

    public int overflow(int currentCount) {
        return Math.max(0, currentCount - max);
    }

    public boolean canAccept(int currentCount, int incomingAmount) {
        return currentCount + incomingAmount <= max;
    }
}
