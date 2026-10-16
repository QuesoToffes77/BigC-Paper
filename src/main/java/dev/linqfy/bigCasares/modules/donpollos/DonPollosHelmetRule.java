package dev.linqfy.bigCasares.modules.donpollos;

/** Cuando el balde KFC puede reemplazar el casco puesto (sin Bukkit). */
final class DonPollosHelmetRule {

    private DonPollosHelmetRule() {
    }

    /**
     * Igual que en vanilla: un casco con Maldicion de ligamiento no se puede sacar, salvo en creativo. Sin esto el
     * balde serviria para sacarse un casco maldito.
     */
    static boolean canReplaceHelmet(boolean helmetHasBindingCurse, boolean creative) {
        return !helmetHasBindingCurse || creative;
    }
}
