package dev.linqfy.bigCasares.modules.forex;

import java.util.List;
import java.util.function.Supplier;

/** Executes one pure market trade and its durable settlement on the leased worker. */
final class ForexTradeActor {
    private ForexTradeActor() {
    }

    static <T> T settle(JournaledForexBank journal, ForexMarket market, ForexStore store,
                        String context, Supplier<T> operation) {
        try {
            journal.context(context);
            T receipt = operation.get();
            ForexSettlement.persist(store, ForexStore.capture(market), journal);
            return receipt;
        } catch (ForexException refusal) {
            if (journal.hasUncommittedTransfer()) journal.pause();
            throw refusal;
        } catch (RuntimeException | Error failure) {
            journal.pause();
            throw failure;
        }
    }

    static List<MarketEvent> tick(JournaledForexBank journal, ForexMarket market, ForexStore store) {
        try {
            journal.context("market tick");
            List<MarketEvent> events = List.copyOf(market.tick());
            if (!events.isEmpty() || journal.hasUncommittedTransfer()) {
                ForexSettlement.persist(store, ForexStore.capture(market), journal);
            }
            return events;
        } catch (RuntimeException | Error failure) {
            journal.pause();
            throw failure;
        }
    }

}
