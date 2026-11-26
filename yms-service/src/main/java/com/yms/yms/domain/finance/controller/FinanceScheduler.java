package com.yms.yms.domain.finance.controller;

import com.yms.yms.domain.finance.service.FinanceService;
import com.yms.yms.domain.finance.service.DebtInterestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class FinanceScheduler {
    private static final Logger log = LoggerFactory.getLogger(FinanceScheduler.class);
    private final FinanceService financeService;
    private final DebtInterestService interestService;
    private final Clock clock;

    public FinanceScheduler(FinanceService financeService, DebtInterestService interestService, Clock clock) {
        this.financeService = financeService;
        this.interestService = interestService;
        this.clock = clock;
    }

    @Scheduled(cron = "${yms.finance.invoice-overdue-cron:0 15 0 * * *}", zone = "${yms.finance.time-zone:Asia/Ho_Chi_Minh}")
    public void markOverdueInvoices() {
        financeService.markOverdue(LocalDate.now(clock));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void accrueInterestOnStartup() {
        accrueInterestThroughToday();
    }

    @Scheduled(cron = "${yms.finance.interest-cron:0 5 0 * * *}", zone = "${yms.finance.time-zone:Asia/Ho_Chi_Minh}")
    public void accrueDailyInterest() {
        accrueInterestThroughToday();
    }

    private void accrueInterestThroughToday() {
        LocalDate today = LocalDate.now(clock);
        int accruedEntries = interestService.accrue(today);
        if (accruedEntries > 0) {
            log.info("Automatically accrued {} daily interest entries through {}", accruedEntries, today);
        }
    }
}
