package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VaultEconomyGatewayTest {

    @Test
    void formatsAmountWithFallbackWhenEconomyUnavailable() {
        assertEquals("$100.00", VaultEconomyGateway.formatFallback(100.0, "$"));
    }
}
