package dev.linqfy.bigCasares.modules.moderation.anticheat;

import java.util.Objects;

/** Lo que un check confirmo tras su buffer: la regla (misma clave que en la config de puntos) y la evidencia. */
public record Detection(String rule, String evidence) {

    public boolean requiresUnattributedAudit() {
        return rule.equals("item-duplicate") || rule.equals("item-duplicate-shared");
    }

    public boolean countsForScore(boolean enforceNewChecks) {
        return !requiresUnattributedAudit() && (enforceNewChecks || switch (rule) {
            case "movement-speed", "upward-movement", "melee-reach" -> true;
            default -> false;
        });
    }

    public Detection {
        Objects.requireNonNull(rule, "rule");
        evidence = evidence == null ? "" : evidence;
    }
}
