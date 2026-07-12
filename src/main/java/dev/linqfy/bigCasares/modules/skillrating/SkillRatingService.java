package dev.linqfy.bigCasares.modules.skillrating;

import com.pocketcombats.openskill.Adjudicator;
import com.pocketcombats.openskill.RatingModelConfig;
import com.pocketcombats.openskill.data.RatingAdjustment;
import com.pocketcombats.openskill.data.SimplePlayerResult;
import com.pocketcombats.openskill.data.SimpleTeamResult;
import com.pocketcombats.openskill.model.ThurstoneMostellerFull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class SkillRatingService {

    private final SkillRatingStorage storage;
    private final SkillRatingSettings settings;
    private final Supplier<Instant> clock;
    private final Adjudicator<UUID> adjudicator;

    public SkillRatingService(SkillRatingStorage storage, SkillRatingSettings settings, Supplier<Instant> clock) {
        this.storage = storage;
        this.settings = settings;
        this.clock = clock;

        RatingModelConfig config = RatingModelConfig.builder().build();
        this.adjudicator = new Adjudicator<>(config, new ThurstoneMostellerFull(config));
    }

    public SkillRatingUpdate recordKill(UUID killerId, UUID victimId) {
        Instant now = clock.get();
        SkillRatingState killer = storage.load(killerId).orElse(SkillRatingState.initial(killerId, settings, now));
        SkillRatingState victim = storage.load(victimId).orElse(SkillRatingState.initial(victimId, settings, now));

        SimplePlayerResult<UUID> killerResult = new SimplePlayerResult<>(killerId, killer.mu(), killer.sigma());
        SimplePlayerResult<UUID> victimResult = new SimplePlayerResult<>(victimId, victim.mu(), victim.sigma());
        List<RatingAdjustment<UUID>> adjustments = adjudicator.rate(List.of(
            new SimpleTeamResult<>(killer.mu(), killer.sigma(), 1, List.of(killerResult)),
            new SimpleTeamResult<>(victim.mu(), victim.sigma(), 2, List.of(victimResult))
        ));

        SkillRatingState updatedKiller = apply(killerId, adjustments, now);
        SkillRatingState updatedVictim = apply(victimId, adjustments, now);

        storage.save(updatedKiller);
        storage.save(updatedVictim);
        return new SkillRatingUpdate(updatedKiller, updatedVictim, killer.tier());
    }

    public SkillRatingState ratingFor(UUID playerId) {
        return storage.load(playerId).orElseGet(() -> SkillRatingState.initial(playerId, settings, clock.get()));
    }

    private SkillRatingState apply(UUID playerId, List<RatingAdjustment<UUID>> adjustments, Instant updatedAt) {
        RatingAdjustment<UUID> adjustment = adjustments.stream()
            .filter(candidate -> candidate.playerId().equals(playerId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("OpenSkill did not return rating adjustment for " + playerId));

        double skillRating = settings.skillRating(adjustment.mu(), adjustment.sigma());
        return new SkillRatingState(
            playerId,
            adjustment.mu(),
            adjustment.sigma(),
            skillRating,
            settings.tierFor(skillRating),
            updatedAt
        );
    }

    public List<SkillRatingState> getTopPlayers(int limit) {
        return storage.loadAll().stream()
                .sorted(java.util.Comparator.comparingDouble(SkillRatingState::skillRating).reversed())
                .limit(limit)
                .toList();
    }

    public SkillRatingSettings getSettings() {
        return settings;
    }
}
