package dev.linqfy.bigCasares.modules.endevent;

public record EndEventDeathResult(boolean participant, boolean eliminated, boolean customHunterMessage) {

    public static EndEventDeathResult ignored() {
        return new EndEventDeathResult(false, false, false);
    }
}
