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
import java.util.UUID;

@Entity
@Table(name = "debt_interest_configs",
        uniqueConstraints = @UniqueConstraint(name = "debt_interest_configs_partner_id_key", columnNames = "partner_id"))
@Getter
@Setter
@NoArgsConstructor
public class DebtInterestConfig {
    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false, unique = true)
    private UUID partnerId;

    @Column(name = "rate_per_day_percent", nullable = false, precision = 8, scale = 5)
    private BigDecimal ratePerDayPercent;

    @Column(name = "grace_days", nullable = false)
    private int graceDays;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
