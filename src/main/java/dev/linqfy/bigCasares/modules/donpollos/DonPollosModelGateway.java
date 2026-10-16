package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.entity.Entity;

interface DonPollosModelGateway {

    ModelHandle attach(Entity anchor, String modelKey);

    /** Igual que attach, pero el modelo crece/encoge con el atributo de escala de la entidad. */
    default ModelHandle attachScaled(Entity anchor, String modelKey) {
        return attach(anchor, modelKey);
    }

    interface ModelHandle {
        boolean animate(String animation);

        void stop(String animation);

        boolean playOnce(String animation, Runnable onEnd);

        void close();

        /** Brillo de encantamiento en todo el modelo (bichos con armadura encantada). */
        default void glint(boolean on) {
        }
    }

    static DonPollosModelGateway unavailable() {
        return (anchor, modelKey) -> {
            throw new IllegalStateException("BetterModel no esta instalado");
        };
    }
}
