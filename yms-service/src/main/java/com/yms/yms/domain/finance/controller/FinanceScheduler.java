package com.yms.yms.domain.finance.controller;

import com.yms.yms.domain.finance.service.FinanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class FinanceScheduler {
    private final FinanceService financeService;
    private final Clock clock;

    public FinanceScheduler(FinanceService financeService, Clock clock) {
        this.financeService = financeService;
        this.clock = clock;
    }

    @Scheduled(cron = "${yms.finance.invoice-overdue-cron:0 15 0 * * *}", zone = "${yms.finance.time-zone:Asia/Ho_Chi_Minh}")
    public void markOverdueInvoices() {
        financeService.markOverdue(LocalDate.now(clock));
    }
}
