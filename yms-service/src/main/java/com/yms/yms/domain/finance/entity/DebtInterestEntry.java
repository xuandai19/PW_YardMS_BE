package com.yms.yms.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "debt_interest_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_debt_interest_day", columnNames = {"debt_id", "accrual_date"}))
@Getter
@Setter
@NoArgsConstructor
public class DebtInterestEntry {
    @Id
    private UUID id;

    @Column(name = "debt_id", nullable = false)
    private UUID debtId;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;

    @Column(name = "rate_per_day_percent", nullable = false, precision = 8, scale = 5)
    private BigDecimal ratePerDayPercent;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
