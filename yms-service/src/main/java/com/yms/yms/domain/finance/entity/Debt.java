package com.yms.yms.domain.finance.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "debts")
@Getter
@Setter
@NoArgsConstructor
public class Debt {
    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "invoice_id", nullable = false, unique = true)
    private UUID invoiceId;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "interest_rate_per_day", nullable = false, precision = 8, scale = 5)
    private BigDecimal interestRatePerDay = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "interest_accrued", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestAccrued = BigDecimal.ZERO;

    @Column(name = "interest_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Amount of the invoice principal which has not yet been paid. */
    @Transient
    @JsonProperty("outstandingPrincipal")
    public BigDecimal getOutstandingPrincipal() {
        return value(principalAmount).subtract(value(paidAmount)).max(BigDecimal.ZERO).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /** Interest that has been charged but has not yet been paid. */
    @Transient
    @JsonProperty("outstandingInterest")
    public BigDecimal getOutstandingInterest() {
        return value(interestAccrued).subtract(value(interestPaid)).max(BigDecimal.ZERO).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /** The exact amount required to settle this debt at the time it is read. */
    @Transient
    @JsonProperty("amountDue")
    public BigDecimal getAmountDue() {
        return getOutstandingPrincipal().add(getOutstandingInterest()).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
