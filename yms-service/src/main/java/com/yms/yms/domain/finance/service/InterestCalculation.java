package com.yms.yms.domain.finance.service;

import java.math.BigDecimal;

public class InterestCalculation {
    private final BigDecimal outstandingPrincipal;
    private final BigDecimal ratePerDayPercent;
    private BigDecimal amount = BigDecimal.ZERO;

    public InterestCalculation(BigDecimal outstandingPrincipal, BigDecimal ratePerDayPercent) {
        this.outstandingPrincipal = outstandingPrincipal;
        this.ratePerDayPercent = ratePerDayPercent;
    }

    public BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public BigDecimal getRatePerDayPercent() {
        return ratePerDayPercent;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
