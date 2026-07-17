package dev.linqfy.bigCasares.items.catalog;

import java.util.Optional;

public interface ItemCatalogReloadOperation {

    Optional<CustomItemCatalog> activeCatalog();

    CustomItemCatalog loadCandidate();

    ItemReconciliationResult commit(CustomItemCatalog candidate);
}
