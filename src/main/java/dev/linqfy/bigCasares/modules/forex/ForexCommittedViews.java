package dev.linqfy.bigCasares.modules.forex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Read-only cross-module views of the last successfully saved market. */
record ForexCommittedViews(Map<UUID, Double> credit, double defaultCredit, Map<String, Set<UUID>> clanPass,
                           Set<UUID> whales) {
    static ForexCommittedViews capture(ForexMarket market) {
        Map<UUID, Double> bestCaps = new HashMap<>();
        Map<String, Set<UUID>> teams = new HashMap<>();
        for (Shitcoin coin : market.coins()) {
            bestCaps.merge(coin.creator(), market.marketCap(coin), Math::max);
            coin.teamId().ifPresent(team -> {
                if (!teams.containsKey(team)) {
                    Set<UUID> holders = new HashSet<>();
                    coin.holdings().forEach((holder, balance) -> {
                        if (balance >= market.settings().clanPassThreshold()) holders.add(holder);
                    });
                    teams.put(team, Set.copyOf(holders));
                }
            });
        }
        Set<UUID> players = new HashSet<>(bestCaps.keySet());
        players.addAll(market.accounts().keySet());
        Map<UUID, Double> credit = new HashMap<>();
        double defaultCredit = multiplier(market, 0, 0);
        for (UUID id : players) {
            ForexMarket.Account account = market.accounts().get(id);
            credit.put(id, multiplier(market, bestCaps.getOrDefault(id, 0.0), account == null ? 0 : account.rugPulls));
        }
        return new ForexCommittedViews(Map.copyOf(credit), defaultCredit, Map.copyOf(teams), Set.copyOf(market.whales()));
    }

    private static double multiplier(ForexMarket market, double best, int rugPulls) {
        double value = 1.0;
        for (Map.Entry<Long, Double> tier : market.settings().credit().prestigeTiers().entrySet()) {
            if (best >= tier.getKey()) value = Math.min(value, tier.getValue());
        }
        value *= 1.0 + rugPulls * market.settings().credit().rugPullPenalty();
        return Math.min(market.settings().credit().maxMultiplier(), value);
    }

    boolean hasClanPass(UUID player, String teamId) {
        return clanPass.getOrDefault(teamId, Set.of()).contains(player);
    }

    double creditMultiplier(UUID player) {
        return credit.getOrDefault(player, defaultCredit);
    }
}
