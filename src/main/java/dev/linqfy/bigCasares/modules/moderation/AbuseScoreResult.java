package dev.linqfy.bigCasares.modules.moderation;

import java.util.Optional;

public record AbuseScoreResult(int score, Optional<AbuseSeverity> alert, boolean punishmentRequested) {

    public AbuseScoreResult {
        alert = alert == null ? Optional.empty() : alert;
    }
}
