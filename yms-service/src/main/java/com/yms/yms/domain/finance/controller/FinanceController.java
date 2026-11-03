package com.yms.yms.domain.finance.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.finance.entity.Debt;
import com.yms.yms.domain.finance.entity.DebtInterestConfig;
import com.yms.yms.domain.finance.entity.DebtInterestEntry;
import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.finance.entity.Payment;
import com.yms.yms.domain.finance.service.DebtInterestService;
import com.yms.yms.domain.finance.service.FinanceService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class FinanceController {
    private final FinanceService financeService;
    private final DebtInterestService interestService;
    private final Clock clock;

    public FinanceController(FinanceService financeService, DebtInterestService interestService, Clock clock) {
        this.financeService = financeService;
        this.interestService = interestService;
        this.clock = clock;
    }

    @GetMapping("/debts")
    public ApiResponse<List<Debt>> debts(@RequestParam(required = false) UUID partnerId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(financeService.debts(partnerId, status));
    }

    @GetMapping("/debts/{id}/interest")
    public ApiResponse<List<DebtInterestEntry>> interestHistory(@PathVariable UUID id) {
        return ApiResponse.ok(interestService.history(id));
    }

    @GetMapping("/interest-config/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<DebtInterestConfig> interestConfig(@PathVariable UUID partnerId) {
        return ApiResponse.ok(financeService.interestConfig(partnerId));
    }

    @PutMapping("/interest-config/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<DebtInterestConfig> configureInterest(@PathVariable UUID partnerId,
            @Valid @RequestBody InterestConfigRequest request) {
        return ApiResponse.ok(financeService.saveInterestConfig(partnerId, request.ratePerDayPercent(),
                request.graceDays(), request.active()));
    }

    @PostMapping("/interest/accrue")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> accrueInterest(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(interestService.accrue(date == null ? LocalDate.now(clock) : date));
    }

    @GetMapping("/invoices")
    public ApiResponse<List<Invoice>> invoices(@RequestParam(required = false) UUID partnerId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(financeService.invoices(partnerId, status));
    }

    @GetMapping("/issueable-orders")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<List<FinanceService.IssueableOrder>> issueableOrders(
            @RequestParam UUID partnerId, @RequestParam(required = false) UUID contractId) {
        return ApiResponse.ok(financeService.issueableOrders(partnerId, contractId));
    }

    @GetMapping("/invoices/{id}")
    public ApiResponse<Invoice> invoice(@PathVariable UUID id) {
        return ApiResponse.ok(financeService.invoice(id));
    }

    @GetMapping("/invoices/{id}/items")
    public ApiResponse<List<FinanceService.InvoiceLine>> invoiceItems(@PathVariable UUID id) {
        return ApiResponse.ok(financeService.invoiceLines(id));
    }

    @PostMapping("/invoices/{id}/e-invoice")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Invoice> submitEInvoice(@PathVariable UUID id) {
        return ApiResponse.ok(financeService.submitEInvoice(id));
    }

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Invoice> issueInvoice(@Valid @RequestBody InvoiceRequest request) {
        return ApiResponse.ok(financeService.issueInvoice(
                new FinanceService.IssueInvoiceRequest(request.partnerId(), request.contractId(), request.serviceOrderIds(),
                        request.dueDate(), request.taxRatePercent())));
    }

    @GetMapping("/invoices/{id}/pdf")
    public ResponseEntity<Resource> invoicePdf(@PathVariable UUID id) {
        Path file = financeService.invoicePdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getFileName().toString()).build().toString())
                .body(new FileSystemResource(file));
    }

    @GetMapping("/invoices/{id}/payments")
    public ApiResponse<List<Payment>> payments(@PathVariable UUID id) {
        return ApiResponse.ok(financeService.payments(id));
    }

    @GetMapping("/payments/{id}/proof")
    public ResponseEntity<Resource> paymentProof(@PathVariable UUID id) {
        Path file = financeService.paymentProof(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.getFileName().toString()).build().toString())
                .body(new FileSystemResource(file));
    }

    @PostMapping(value = "/invoices/{id}/payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Payment> recordPayment(@PathVariable UUID id,
            @RequestParam @DecimalMin(value = "0.0001") BigDecimal amount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant paidAt,
            @RequestParam(required = false) String referenceNo, @RequestParam(required = false) String note,
            @RequestPart(required = false) MultipartFile proof) {
        return ApiResponse.ok(financeService.recordPayment(id, amount, paidAt, referenceNo, note, proof));
    }

    @GetMapping("/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<FinanceService.Reconciliation> reconcile(@RequestParam UUID partnerAId,
            @RequestParam UUID partnerCId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(financeService.reconcile(partnerAId, partnerCId, from, to));
    }

    public record InvoiceRequest(@NotNull UUID partnerId, UUID contractId, @NotEmpty List<@NotNull UUID> serviceOrderIds,
            @NotNull LocalDate dueDate, @DecimalMin("0.0") BigDecimal taxRatePercent) {
    }

    public record InterestConfigRequest(@NotNull @DecimalMin("0.0") BigDecimal ratePerDayPercent,
            @Min(0) int graceDays, boolean active) {
    }
}
