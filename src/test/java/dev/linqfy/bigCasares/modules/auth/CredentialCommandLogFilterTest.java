package dev.linqfy.bigCasares.modules.auth;

import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CredentialCommandLogFilterTest {
    private final CredentialCommandLogGuard.Filter filter = new CredentialCommandLogGuard.Filter();

    @Test
    void suppressesOnlyPaperCredentialCommandRecordsIncludingAliasesAndNamespaces() {
        for (String command : new String[] {"/login x", "/L x", "/log x", "/register x", "/reg x",
            "/auth x", "/cuenta x", "/bigcasares:login x", "/BIGCASARES:CUENTA x"}) {
            assertEquals(Result.DENY, filter.filter(event(CredentialCommandLogGuard.PAPER_LOGGER,
                "{} issued server command: {}", command)), command);
        }
    }

    @Test
    void unrelatedCommandsLoggersAndMessagesRemainVisible() {
        assertEquals(Result.NEUTRAL, filter.filter(event(CredentialCommandLogGuard.PAPER_LOGGER,
            "{} issued server command: {}", "/spawn")));
        assertEquals(Result.NEUTRAL, filter.filter(event(CredentialCommandLogGuard.PAPER_LOGGER,
            "{} issued server command: {}", "/login-extra x")));
        assertEquals(Result.NEUTRAL, filter.filter(event("other.logger", "{} issued server command: {}", "/login x")));
        assertEquals(Result.NEUTRAL, filter.filter(event(CredentialCommandLogGuard.PAPER_LOGGER,
            "{} another audit event: {}", "/login x")));
    }

    private static Log4jLogEvent event(String logger, String format, String command) {
        return Log4jLogEvent.newBuilder().setLoggerName(logger)
            .setMessage(new ParameterizedMessage(format, "Player", command)).build();
    }
}
