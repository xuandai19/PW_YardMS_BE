package com.yms.yms.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_service_orders")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceServiceOrder {
    @EmbeddedId
    private InvoiceServiceOrderId id;

    @Column(name = "line_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineAmount;
}
