package dev.linqfy.bigCasares.modules.skillrating;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillRatingServiceTest {

    @Test
    void recordsKillerWinAndMapsRatingToTier() {
        InMemorySkillRatingStorage storage = new InMemorySkillRatingStorage();
        SkillRatingSettings settings = new SkillRatingSettings(25.0, 25.0 / 3.0, 3.0, 100.0, new double[] {500.0, 1500.0, 2500.0, 3500.0});
        SkillRatingService service = new SkillRatingService(storage, settings, () -> Instant.parse("2026-04-24T12:00:00Z"));

        UUID killerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID victimId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        SkillRatingUpdate update = service.recordKill(killerId, victimId);

        assertTrue(update.killerState().skillRating() > 500.0);
        assertTrue(update.killerState().skillRating() > update.victimState().skillRating());
        assertEquals(2, update.killerState().tier());
        assertEquals(update.killerState(), storage.load(killerId).orElseThrow());
        assertEquals(update.victimState(), storage.load(victimId).orElseThrow());
    }

    @Test
    void clampsTierBetweenOneAndFive() {
        SkillRatingSettings settings = new SkillRatingSettings(25.0, 25.0 / 3.0, 3.0, 100.0, new double[] {500.0, 1500.0, 2500.0, 3500.0});

        assertEquals(1, settings.tierFor(499.99));
        assertEquals(2, settings.tierFor(500.0));
        assertEquals(5, settings.tierFor(9900.0));
    }

    @Test
    void scalesPublicSkillRatingWithoutChangingOpenSkillInputs() {
        SkillRatingSettings settings = new SkillRatingSettings(25.0, 25.0 / 3.0, 3.0, 100.0, new double[] {500.0, 1500.0, 2500.0, 3500.0});

        assertEquals(1000.0, settings.skillRating(35.0, 25.0 / 3.0), 0.0001);
    }

    @Test
    void formatsRatingCommandLine() {
        SkillRatingState state = new SkillRatingState(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            31.234,
            7.891,
            756.78,
            2,
            Instant.parse("2026-04-24T12:00:00Z")
        );

        assertEquals(
            "Skill Rating de linqfy: 757 | Tier 2 | mu 31.23 | sigma 7.89",
            SkillRatingView.format("linqfy", state)
        );
    }

    private static final class InMemorySkillRatingStorage implements SkillRatingStorage {
        private final Map<UUID, SkillRatingState> states = new HashMap<>();

        @Override
        public Optional<SkillRatingState> load(UUID playerId) {
            return Optional.ofNullable(states.get(playerId));
        }

        @Override
        public void save(SkillRatingState state) {
            states.put(state.playerId(), state);
        }
    }
}
