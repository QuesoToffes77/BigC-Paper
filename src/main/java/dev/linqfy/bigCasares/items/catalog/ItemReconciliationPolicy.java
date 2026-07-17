package dev.linqfy.bigCasares.items.catalog;

import java.util.Objects;

public final class ItemReconciliationPolicy {

    public ItemReconciliationDecision decide(
        String logicalId,
        CustomItemCatalog previous,
        CustomItemCatalog candidate
    ) {
        Objects.requireNonNull(logicalId, "logicalId");
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.find(logicalId).isPresent()) {
            return ItemReconciliationDecision.REFRESH;
        }
        if (previous != null && previous.find(logicalId).isPresent()) {
            return ItemReconciliationDecision.MARK_LEGACY;
        }
        return ItemReconciliationDecision.IGNORE;
    }
}
