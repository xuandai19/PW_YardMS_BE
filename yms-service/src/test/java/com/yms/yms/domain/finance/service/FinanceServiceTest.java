package com.yms.yms.domain.finance.service;

import com.yms.yms.domain.contract.repository.ContractRepository;
import com.yms.yms.domain.finance.entity.Debt;
import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.finance.repository.DebtInterestConfigRepository;
import com.yms.yms.domain.finance.repository.DebtRepository;
import com.yms.yms.domain.finance.repository.InvoiceRepository;
import com.yms.yms.domain.finance.repository.InvoiceServiceOrderRepository;
import com.yms.yms.domain.finance.repository.PaymentRepository;
import com.yms.yms.domain.service.repository.ServiceOrderRepository;
import com.yms.yms.infrastructure.storage.ContractStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceServiceTest {
    private InvoiceRepository invoices;
    private PaymentRepository payments;
    private DebtRepository debts;
    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        invoices = mock(InvoiceRepository.class);
        ContractRepository contracts = mock(ContractRepository.class);
        InvoiceServiceOrderRepository invoiceOrders = mock(InvoiceServiceOrderRepository.class);
        payments = mock(PaymentRepository.class);
        debts = mock(DebtRepository.class);
        DebtInterestConfigRepository interestConfigs = mock(DebtInterestConfigRepository.class);
        ServiceOrderRepository serviceOrders = mock(ServiceOrderRepository.class);
        ContractStorageService storage = mock(ContractStorageService.class);
        InvoiceDocumentService documents = mock(InvoiceDocumentService.class);
        EInvoiceGateway eInvoiceGateway = mock(EInvoiceGateway.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-10T12:00:00Z"), ZoneOffset.UTC);
        financeService = new FinanceService(invoices, invoiceOrders, payments, debts, interestConfigs,
                serviceOrders, contracts, storage, documents, eInvoiceGateway, jdbc, clock);
    }

    @Test
    void finalPartialPaymentUpdatesInvoiceAndDebtWithoutExceedingPrincipal() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTotalAmount(new BigDecimal("100.0000"));
        invoice.setDueDate(LocalDate.of(2026, 6, 1));
        invoice.setStatus("UNPAID");
        Debt debt = new Debt();
        debt.setId(UUID.randomUUID());
        debt.setInvoiceId(invoiceId);
        debt.setPrincipalAmount(new BigDecimal("100.0000"));
        debt.setPaidAmount(new BigDecimal("75.0000"));
        when(invoices.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(payments.sumForInvoice(invoiceId)).thenReturn(new BigDecimal("75.0000"));
        when(debts.findByInvoiceId(invoiceId)).thenReturn(Optional.of(debt));

        financeService.recordPayment(invoiceId, new BigDecimal("25.0000"),
                Instant.parse("2026-05-10T11:00:00Z"), "BANK-REF", null, null);

        assertEquals("PAID", invoice.getStatus());
        assertEquals(new BigDecimal("100.0000"), debt.getPaidAmount());
        assertEquals("PAID", debt.getStatus());
        verify(payments).save(any());
        verify(debts).save(debt);
    }

    @Test
    void acceptsPaymentForAccruedInterestAfterInvoicePrincipalWasPaid() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTotalAmount(new BigDecimal("100.0000"));
        invoice.setDueDate(LocalDate.of(2026, 5, 1));
        invoice.setStatus("PAID");
        Debt debt = new Debt();
        debt.setId(UUID.randomUUID());
        debt.setInvoiceId(invoiceId);
        debt.setPrincipalAmount(new BigDecimal("100.0000"));
        debt.setPaidAmount(new BigDecimal("100.0000"));
        debt.setInterestAccrued(new BigDecimal("5.0000"));
        when(invoices.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(debts.findByInvoiceId(invoiceId)).thenReturn(Optional.of(debt));

        financeService.recordPayment(invoiceId, new BigDecimal("5.0000"),
                Instant.parse("2026-05-10T11:00:00Z"), "INTEREST-REF", null, null);

        assertEquals("PAID", invoice.getStatus());
        assertEquals(new BigDecimal("5.0000"), debt.getInterestPaid());
        assertEquals("PAID", debt.getStatus());
    }

    @Test
    void rejectsPaymentAboveRemainingPrincipalAndInterest() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setTotalAmount(new BigDecimal("100.0000"));
        invoice.setDueDate(LocalDate.of(2026, 6, 1));
        Debt debt = new Debt();
        debt.setId(UUID.randomUUID());
        debt.setInvoiceId(invoiceId);
        debt.setPrincipalAmount(new BigDecimal("100.0000"));
        debt.setPaidAmount(new BigDecimal("75.0000"));
        when(invoices.findByIdForUpdate(invoiceId)).thenReturn(Optional.of(invoice));
        when(debts.findByInvoiceId(invoiceId)).thenReturn(Optional.of(debt));

        assertThrows(com.yms.common.exception.BusinessException.class, () -> financeService.recordPayment(invoiceId,
                new BigDecimal("25.0001"), Instant.parse("2026-05-10T11:00:00Z"), null, null, null));
    }
}
