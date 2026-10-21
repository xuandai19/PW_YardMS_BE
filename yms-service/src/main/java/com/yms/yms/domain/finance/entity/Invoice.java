package com.yms.yms.domain.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
    @Id
    private UUID id;

    @Column(name = "invoice_no", nullable = false, unique = true, length = 80)
    private String invoiceNo;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 20)
    private String status = "UNPAID";

    @Column(name = "pdf_url", length = 1000)
    @JsonIgnore
    private String pdfUrl;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "invoice_type", nullable = false, length = 20)
    private String invoiceType = "SERVICE";

    @Column(name = "e_invoice_provider_id", length = 150)
    private String eInvoiceProviderId;

    @Column(name = "e_invoice_status", nullable = false, length = 30)
    private String eInvoiceStatus = "NOT_SUBMITTED";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivery_status", nullable = false, length = 20)
    private String deliveryStatus = "NOT_SENT";

    @Column(name = "delivery_error", length = 500)
    private String deliveryError;

    @Column(name = "delivered_at")
    private Instant deliveredAt;
}
