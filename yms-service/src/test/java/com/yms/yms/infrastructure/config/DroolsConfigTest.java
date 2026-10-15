package com.yms.yms.infrastructure.config;

import com.yms.yms.domain.finance.service.InterestCalculation;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;
import org.kie.api.runtime.KieSession;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DroolsConfigTest {
    @Test
    void calculatesDailyInterestAsPercentOfOutstandingPrincipal() throws Exception {
        KieBase base = new DroolsConfig().debtInterestKieBase();
        KieSession session = base.newKieSession();
        try {
            InterestCalculation calculation = new InterestCalculation(
                    new BigDecimal("10000.0000"), new BigDecimal("1.25000"));
            session.insert(calculation);
            session.fireAllRules();
            assertEquals(new BigDecimal("125.0000"), calculation.getAmount());
        } finally {
            session.dispose();
        }
    }
}
