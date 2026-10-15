package com.yms.yms.domain.finance.repository;

import com.yms.yms.domain.finance.entity.DebtInterestEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DebtInterestEntryRepository extends JpaRepository<DebtInterestEntry, UUID> {
    boolean existsByDebtIdAndAccrualDate(UUID debtId, LocalDate accrualDate);
    List<DebtInterestEntry> findByDebtIdOrderByAccrualDateAsc(UUID debtId);
}
