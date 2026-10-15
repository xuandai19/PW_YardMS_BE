package com.yms.yms.domain.service.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceDispatchServiceTest {

    @Test
    void calculatesHourlyCostUsingExactElapsedTime() {
        assertEquals(new BigDecimal("125.0000"),
                ServiceDispatchService.calculateCost(new BigDecimal("500.0000"), 15 * 60 * 1000L));
        assertEquals(new BigDecimal("500.0000"),
                ServiceDispatchService.calculateCost(new BigDecimal("500.0000"), 60 * 60 * 1000L));
    }
}
