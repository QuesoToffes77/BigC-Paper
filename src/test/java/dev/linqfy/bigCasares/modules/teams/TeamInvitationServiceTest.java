package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamInvitationServiceTest {

    private static final TeamId TEAM = new TeamId(
        UUID.fromString("50000000-0000-0000-0000-000000000001"));
    private static final TeamId OTHER_TEAM = new TeamId(
        UUID.fromString("50000000-0000-0000-0000-000000000002"));
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000051");
    private static final UUID OTHER_PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000052");

    @Test
    void invitationCanOnlyBeConsumedOnceByItsInvitedPlayer() {
        TeamInvitationService invitations = serviceAt("2026-07-11T12:00:00Z");
        invitations.invite(TEAM, PLAYER);

        assertFalse(invitations.consume(OTHER_TEAM, PLAYER));
        assertFalse(invitations.consume(TEAM, OTHER_PLAYER));
        assertTrue(invitations.consume(TEAM, PLAYER));
        assertFalse(invitations.consume(TEAM, PLAYER));
    }

    @Test
    void expiredInvitationCannotBeConsumed() {
        Instant issuedAt = Instant.parse("2026-07-11T12:00:00Z");
        MutableClock clock = new MutableClock(issuedAt);
        TeamInvitationService invitations = new TeamInvitationService(clock, Duration.ofMinutes(5));
        invitations.invite(TEAM, PLAYER);

        clock.current = issuedAt.plus(Duration.ofMinutes(5)).plusMillis(1);

        assertFalse(invitations.consume(TEAM, PLAYER));
    }

    @Test
    void invitationsCanBeClearedForPlayerOrTeam() {
        TeamInvitationService invitations = serviceAt("2026-07-11T12:00:00Z");
        invitations.invite(TEAM, PLAYER);
        invitations.invite(OTHER_TEAM, PLAYER);
        invitations.invite(TEAM, OTHER_PLAYER);

        invitations.clearPlayer(PLAYER);

        assertFalse(invitations.consume(TEAM, PLAYER));
        assertFalse(invitations.consume(OTHER_TEAM, PLAYER));
        assertTrue(invitations.hasValidInvitation(TEAM, OTHER_PLAYER));

        invitations.clearTeam(TEAM);

        assertFalse(invitations.hasValidInvitation(TEAM, OTHER_PLAYER));
    }

    private static TeamInvitationService serviceAt(String instant) {
        return new TeamInvitationService(
            Clock.fixed(Instant.parse(instant), ZoneOffset.UTC),
            Duration.ofMinutes(5)
        );
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
