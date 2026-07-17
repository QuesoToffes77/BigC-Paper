package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCatalogReloadMessageFormatterTest {

    @Test
    void reportsFailuresWithoutClaimingACommit() {
        ItemCatalogReloadResult result = new ItemCatalogReloadResult(
            ItemCatalogReloadStatus.FAILED, Duration.ZERO, null, null, null,
            new IllegalArgumentException("modelo duplicado")
        );

        String message = ItemCatalogReloadMessageFormatter.format(result);

        assertTrue(message.contains("No se pudo"));
        assertTrue(message.contains("modelo duplicado"));
    }
}
