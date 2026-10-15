package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.finance.entity.Debt;
import com.yms.yms.domain.finance.repository.DebtInterestConfigRepository;
import com.yms.yms.domain.finance.repository.DebtInterestEntryRepository;
import com.yms.yms.domain.finance.repository.DebtRepository;
import com.yms.yms.infrastructure.config.DroolsConfig;
import org.junit.jupiter.api.Test;
import org.kie.api.KieBase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DebtInterestServiceTest {
    @Test
    void accruesDailyInterestOncePerDebtAndDate() throws Exception {
        DebtRepository debts = mock(DebtRepository.class);
        DebtInterestConfigRepository configs = mock(DebtInterestConfigRepository.class);
        DebtInterestEntryRepository entries = mock(DebtInterestEntryRepository.class);
        KieBase kieBase = new DroolsConfig().debtInterestKieBase();
        Debt debt = new Debt();
        debt.setId(UUID.randomUUID());
        debt.setPartnerId(UUID.randomUUID());
        debt.setPrincipalAmount(new BigDecimal("10000.0000"));
        debt.setPaidAmount(new BigDecimal("2500.0000"));
        debt.setInterestRatePerDay(new BigDecimal("1.00000"));
        debt.setDueDate(LocalDate.of(2026, 5, 8));
        debt.setInterestAccrued(BigDecimal.ZERO);
        debt.setInterestPaid(BigDecimal.ZERO);
        when(debts.findByStatusOrderByDueDateAsc("OPEN")).thenReturn(List.of(debt));
        when(configs.findByPartnerId(debt.getPartnerId())).thenReturn(Optional.empty());
        when(entries.existsByDebtIdAndAccrualDate(debt.getId(), LocalDate.of(2026, 5, 10)))
                .thenReturn(false, true);
        DebtInterestService interestService = new DebtInterestService(debts, configs, entries, kieBase,
                Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC));

        assertEquals(1, interestService.accrue(LocalDate.of(2026, 5, 10)));
        assertEquals(0, interestService.accrue(LocalDate.of(2026, 5, 10)));
        assertEquals(new BigDecimal("75.0000"), debt.getInterestAccrued());
        verify(entries, times(1)).save(org.mockito.ArgumentMatchers.any());
    }
}
