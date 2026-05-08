package com.credito.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreditoServiceNamesTest {

    @Test
    void definesServiceNames() {
        assertEquals("gateway-service", CreditoServiceNames.GATEWAY_SERVICE);
        assertEquals("customer-service", CreditoServiceNames.CUSTOMER_SERVICE);
        assertEquals("account-service", CreditoServiceNames.ACCOUNT_SERVICE);
        assertEquals("lending-service", CreditoServiceNames.LENDING_SERVICE);
        assertEquals("batch-service", CreditoServiceNames.BATCH_SERVICE);
    }
}
