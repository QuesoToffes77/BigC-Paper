package dev.linqfy.bigCasares.modules.forex;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ForexCommittedViewsTest {
    @Test
    void frozenViewsMatchMarketAndDoNotChangeWithSubsequentMutations() {
        UUID creator = UUID.randomUUID();
        UUID holder = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("forex.credit.prestige-tiers.0", 0.8);
        ForexMarket market = new ForexMarket(ForexSettings.load(yaml), new FakeBank(), new Random(1), () -> 1L);
        Shitcoin coin = new Shitcoin("TEST", "Test", creator, 1, 0.03, 0.5);
        coin.teamId("team"); coin.restoreHolding(holder, 1_000, 0, 0); coin.supply(1_000); coin.pool(100_000);
        market.restoreCoin(coin);
        ForexMarket.Account account = new ForexMarket.Account(); account.rugPulls = 1;
        market.accounts().put(creator, account);
        ForexCommittedViews views = ForexCommittedViews.capture(market);
        assertEquals(market.creditMultiplier(creator), views.creditMultiplier(creator), 1e-12);
        assertEquals(market.creditMultiplier(stranger), views.creditMultiplier(stranger), 1e-12);
        assertEquals(market.hasClanPass(holder, "team"), views.hasClanPass(holder, "team"));
        coin.debit(holder, 1_000); account.rugPulls = 10;
        assertTrue(views.hasClanPass(holder, "team"));
        assertNotEquals(market.creditMultiplier(creator), views.creditMultiplier(creator));
    }
}
