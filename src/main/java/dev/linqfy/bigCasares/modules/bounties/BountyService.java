package dev.linqfy.bigCasares.modules.bounties;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class BountyService {

    private final BountyStorage storage;
    private final BountyEconomy economy;
    private final BountySettings settings;
    private final Supplier<Instant> clock;

    public BountyService(BountyStorage storage, BountyEconomy economy, BountySettings settings, Supplier<Instant> clock) {
        this.storage = storage;
        this.economy = economy;
        this.settings = settings;
        this.clock = clock;
    }

    public BountyProcessingResult handlePlayerKill(UUID victimId, UUID killerId) {
        BountyPlayerState current = storage.load(victimId).orElse(BountyPlayerState.empty(victimId, clock.get()));
        double paidOut = current.activeBounty();

        if (paidOut > 0.0) {
            economy.deposit(killerId, paidOut);
        }

        double balance = economy.balance(victimId);
        if (balance < settings.minimumBalance()) {
            storage.save(new BountyPlayerState(victimId, 0.0, clock.get()));
            return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false, paidOut > 0.0);
        }

        double newBounty = balance * settings.bountyPercent();
        if (newBounty < settings.minimumBounty()) {
            storage.save(new BountyPlayerState(victimId, 0.0, clock.get()));
            return new BountyProcessingResult(paidOut, 0.0, 0.0, 0.0, false, paidOut > 0.0);
        }

        double totalTaken = balance * settings.takePercent();
        double bleed = totalTaken - newBounty;

        economy.withdraw(victimId, totalTaken);
        BountyPlayerState updated = new BountyPlayerState(victimId, newBounty, clock.get());
        storage.save(updated);
        return new BountyProcessingResult(paidOut, newBounty, totalTaken, bleed, true, paidOut > 0.0);
    }

    public BountyPlayerState stackGeneratedBounty(UUID victimId, double amount) {
        BountyPlayerState current = storage.load(victimId).orElse(BountyPlayerState.empty(victimId, clock.get()));
        BountyPlayerState updated = new BountyPlayerState(victimId, current.activeBounty() + amount, clock.get());
        storage.save(updated);
        return updated;
    }

    public List<BountyPlayerState> topBounties(int limit) {
        if (limit < 1) {
            return List.of();
        }
        return storage.all()
                .filter(state -> state.activeBounty() > 0.0)
                .sorted(Comparator.comparingDouble(BountyPlayerState::activeBounty).reversed())
                .limit(limit)
                .toList();
    }
}
