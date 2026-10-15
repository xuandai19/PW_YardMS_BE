package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.finance.entity.Debt;
import com.yms.yms.domain.finance.entity.DebtInterestConfig;
import com.yms.yms.domain.finance.entity.DebtInterestEntry;
import com.yms.yms.domain.finance.repository.DebtInterestConfigRepository;
import com.yms.yms.domain.finance.repository.DebtInterestEntryRepository;
import com.yms.yms.domain.finance.repository.DebtRepository;
import org.kie.api.KieBase;
import jakarta.transaction.Transactional;
import org.kie.api.runtime.KieSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DebtInterestService {
    private final DebtRepository debts;
    private final DebtInterestConfigRepository configs;
    private final DebtInterestEntryRepository entries;
    private final KieBase kieBase;
    private final Clock clock;

    public DebtInterestService(DebtRepository debts, DebtInterestConfigRepository configs,
            DebtInterestEntryRepository entries, KieBase kieBase, Clock clock) {
        this.debts = debts;
        this.configs = configs;
        this.entries = entries;
        this.kieBase = kieBase;
        this.clock = clock;
    }

    public BigDecimal calculate(BigDecimal outstandingPrincipal, BigDecimal ratePerDayPercent) {
        if (outstandingPrincipal == null || ratePerDayPercent == null
                || outstandingPrincipal.signum() < 0 || ratePerDayPercent.signum() < 0) {
            throw new IllegalArgumentException("Principal and interest rate must not be negative");
        }
        KieSession session = kieBase.newKieSession();
        try {
            InterestCalculation calculation = new InterestCalculation(outstandingPrincipal, ratePerDayPercent);
            session.insert(calculation);
            session.fireAllRules();
            return calculation.getAmount();
        } finally {
            session.dispose();
        }
    }

    @Transactional
    public int accrue(LocalDate accrualDate) {
        int count = 0;
        for (Debt debt : debts.findByStatusOrderByDueDateAsc("OPEN")) {
            BigDecimal principal = debt.getPrincipalAmount().subtract(debt.getPaidAmount()).max(BigDecimal.ZERO);
            if (principal.signum() == 0 || !accrualDate.isAfter(debt.getDueDate())) {
                continue;
            }
            DebtInterestConfig config = configs.findByPartnerId(debt.getPartnerId()).orElse(null);
            if (config != null && !config.isActive()) {
                continue;
            }
            int graceDays = config == null ? 0 : config.getGraceDays();
            if (!accrualDate.isAfter(debt.getDueDate().plusDays(graceDays))
                    || entries.existsByDebtIdAndAccrualDate(debt.getId(), accrualDate)) {
                continue;
            }
            BigDecimal rate = config == null ? debt.getInterestRatePerDay() : config.getRatePerDayPercent();
            if (rate.signum() == 0) {
                continue;
            }
            BigDecimal interest = calculate(principal, rate);
            DebtInterestEntry entry = new DebtInterestEntry();
            entry.setId(UUID.randomUUID());
            entry.setDebtId(debt.getId());
            entry.setAccrualDate(accrualDate);
            entry.setOutstandingPrincipal(principal);
            entry.setRatePerDayPercent(rate);
            entry.setAmount(interest);
            entry.setCreatedAt(clock.instant());
            entries.save(entry);
            debt.setInterestAccrued(debt.getInterestAccrued().add(interest).setScale(4, RoundingMode.HALF_UP));
            debt.setUpdatedAt(clock.instant());
            debts.save(debt);
            count++;
        }
        return count;
    }

    @Transactional
    @Scheduled(cron = "${yms.finance.interest-cron:0 5 0 * * *}", zone = "${yms.finance.time-zone:Asia/Ho_Chi_Minh}")
    public void accrueDaily() {
        accrue(LocalDate.now(clock));
    }

    public List<DebtInterestEntry> history(UUID debtId) {
        return entries.findByDebtIdOrderByAccrualDateAsc(debtId);
    }
}
