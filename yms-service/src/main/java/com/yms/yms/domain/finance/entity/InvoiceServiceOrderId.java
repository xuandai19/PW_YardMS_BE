package com.yms.yms.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class InvoiceServiceOrderId implements Serializable {
    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "service_order_id")
    private UUID serviceOrderId;
}
