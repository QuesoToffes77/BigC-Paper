package dev.linqfy.bigCasares.modules.shop;

final class ShopFormGeneration {
    private long current;
    private boolean active;

    synchronized long activateNext() {
        current++;
        active = true;
        return current;
    }

    synchronized void invalidate() {
        current++;
        active = false;
    }

    synchronized long current() {
        return current;
    }

    synchronized boolean accepts(long expectedGeneration, boolean playerOnline) {
        return active && playerOnline && current == expectedGeneration;
    }
}
