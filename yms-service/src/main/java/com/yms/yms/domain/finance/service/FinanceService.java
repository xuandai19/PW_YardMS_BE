package com.yms.yms.domain.finance.service;

import com.yms.common.exception.BusinessException;
import com.yms.yms.domain.contract.entity.Contract;
import com.yms.yms.domain.contract.repository.ContractRepository;
import com.yms.yms.domain.finance.entity.Debt;
import com.yms.yms.domain.finance.entity.DebtInterestConfig;
import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.finance.entity.InvoiceServiceOrder;
import com.yms.yms.domain.finance.entity.InvoiceServiceOrderId;
import com.yms.yms.domain.finance.entity.Payment;
import com.yms.yms.domain.finance.repository.DebtInterestConfigRepository;
import com.yms.yms.domain.finance.repository.DebtRepository;
import com.yms.yms.domain.finance.repository.InvoiceRepository;
import com.yms.yms.domain.finance.repository.InvoiceServiceOrderRepository;
import com.yms.yms.domain.finance.repository.PaymentRepository;
import com.yms.yms.domain.service.entity.ServiceOrder;
import com.yms.yms.domain.service.repository.ServiceOrderRepository;
import com.yms.yms.infrastructure.storage.ContractStorageService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.IOException;

@Service
public class FinanceService {
    private final InvoiceRepository invoices;
    private final ContractRepository contracts;
    private final InvoiceServiceOrderRepository invoiceOrders;
    private final PaymentRepository payments;
    private final DebtRepository debts;
    private final DebtInterestConfigRepository interestConfigs;
    private final ServiceOrderRepository serviceOrders;
    private final ContractStorageService storage;
    private final InvoiceDocumentService invoiceDocuments;
    private final EInvoiceGateway eInvoiceGateway;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public FinanceService(InvoiceRepository invoices, InvoiceServiceOrderRepository invoiceOrders,
            PaymentRepository payments, DebtRepository debts, DebtInterestConfigRepository interestConfigs,
            ServiceOrderRepository serviceOrders, ContractRepository contracts, ContractStorageService storage,
            InvoiceDocumentService invoiceDocuments, EInvoiceGateway eInvoiceGateway, JdbcTemplate jdbc, Clock clock) {
        this.invoices = invoices;
        this.contracts = contracts;
        this.invoiceOrders = invoiceOrders;
        this.payments = payments;
        this.debts = debts;
        this.interestConfigs = interestConfigs;
        this.serviceOrders = serviceOrders;
        this.storage = storage;
        this.invoiceDocuments = invoiceDocuments;
        this.eInvoiceGateway = eInvoiceGateway;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional
    public Invoice issueInvoice(IssueInvoiceRequest request) {
        if (request.partnerId() == null || request.dueDate() == null) {
            throw new BusinessException("Partner and due date are required", HttpStatus.BAD_REQUEST);
        }
        if (!request.dueDate().isAfter(LocalDate.now(clock))) {
            throw new BusinessException("Invoice due date must be in the future", HttpStatus.BAD_REQUEST);
        }
        if (request.serviceOrderIds() == null || request.serviceOrderIds().isEmpty()
                || request.serviceOrderIds().size() != request.serviceOrderIds().stream().distinct().count()) {
            throw new BusinessException("Provide distinct service orders to invoice", HttpStatus.BAD_REQUEST);
        }
        List<ServiceOrder> orders = new ArrayList<>();
        for (UUID orderId : request.serviceOrderIds()) {
            ServiceOrder order = serviceOrders.findById(orderId)
                    .orElseThrow(() -> new BusinessException("Service order not found: " + orderId, HttpStatus.NOT_FOUND));
            if (!"COMPLETED".equals(order.getStatus()) || invoiceOrders.existsByIdServiceOrderId(orderId)) {
                throw new BusinessException("Only completed, uninvoiced service orders can be invoiced",
                        HttpStatus.CONFLICT);
            }
            List<UUID> partnerIds = jdbc.query("""
                    SELECT pn.partner_a_id
                    FROM service_orders so
                    JOIN trips t ON t.id = so.trip_id
                    JOIN pre_notifications pn ON pn.trip_id = t.id
                    WHERE so.id = ?
                    """, (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class), orderId);
            if (partnerIds.isEmpty()) {
                throw new BusinessException("Service order is not linked to a pre-notification",
                        HttpStatus.CONFLICT);
            }
            UUID sourcePartnerA = partnerIds.get(0);
            if (!request.partnerId().equals(sourcePartnerA)) {
                Contract contract = request.contractId() == null ? null : contracts.findById(request.contractId())
                        .orElseThrow(() -> new BusinessException("Contract not found", HttpStatus.NOT_FOUND));
                if (contract == null || !"ACTIVE".equals(contract.getStatus())
                        || !contract.getPartnerAId().equals(sourcePartnerA)
                        || !request.partnerId().equals(contract.getPartnerCId())) {
                    throw new BusinessException("Invoices for partner C require an active A-C contract",
                            HttpStatus.BAD_REQUEST);
                }
                if (LocalDate.now(clock).isBefore(contract.getStartDate())
                        || LocalDate.now(clock).isAfter(contract.getEndDate())) {
                    throw new BusinessException("The A-C contract is not valid on the invoice date",
                            HttpStatus.CONFLICT);
                }
            } else if (request.contractId() != null) {
                Contract contract = contracts.findById(request.contractId())
                        .orElseThrow(() -> new BusinessException("Contract not found", HttpStatus.NOT_FOUND));
                if (!"ACTIVE".equals(contract.getStatus()) || !sourcePartnerA.equals(contract.getPartnerAId())
                        || LocalDate.now(clock).isBefore(contract.getStartDate())
                        || LocalDate.now(clock).isAfter(contract.getEndDate())) {
                    throw new BusinessException("Contract is not active for this partner and invoice date",
                            HttpStatus.CONFLICT);
                }
            }
            orders.add(order);
        }
        BigDecimal subtotal = orders.stream().map(ServiceOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
        BigDecimal taxRate = request.taxRatePercent() == null ? BigDecimal.ZERO : request.taxRatePercent();
        if (taxRate.signum() < 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Tax rate must be between 0 and 100 percent", HttpStatus.BAD_REQUEST);
        }
        BigDecimal tax = subtotal.multiply(taxRate).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        Instant now = clock.instant();
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNo(String.format("YMS-%d-%06d", LocalDate.now(clock).getYear(),
                jdbc.queryForObject("SELECT nextval('invoice_number_seq')", Long.class)));
        invoice.setPartnerId(request.partnerId());
        invoice.setContractId(request.contractId());
        invoice.setIssueDate(LocalDate.now(clock));
        invoice.setDueDate(request.dueDate());
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(tax);
        invoice.setTotalAmount(subtotal.add(tax).setScale(4, RoundingMode.HALF_UP));
        invoice.setStatus("UNPAID");
        invoice.setInvoiceType("SERVICE");
        invoice.setIssuedAt(now);
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);
        invoice.setPdfUrl(invoiceDocuments.generate(invoice, orders));
        invoices.save(invoice);
        for (ServiceOrder order : orders) {
            InvoiceServiceOrder line = new InvoiceServiceOrder();
            InvoiceServiceOrderId lineId = new InvoiceServiceOrderId();
            lineId.setInvoiceId(invoice.getId());
            lineId.setServiceOrderId(order.getId());
            line.setId(lineId);
            line.setLineAmount(order.getTotalAmount());
            invoiceOrders.save(line);
        }
        Debt debt = new Debt();
        debt.setId(UUID.randomUUID());
        debt.setPartnerId(invoice.getPartnerId());
        debt.setInvoiceId(invoice.getId());
        debt.setPrincipalAmount(invoice.getTotalAmount());
        debt.setPaidAmount(BigDecimal.ZERO);
        debt.setInterestRatePerDay(interestConfigs.findByPartnerId(invoice.getPartnerId())
                .filter(DebtInterestConfig::isActive)
                .map(DebtInterestConfig::getRatePerDayPercent).orElse(BigDecimal.ZERO));
        debt.setDueDate(invoice.getDueDate());
        debt.setStatus("OPEN");
        debt.setCreatedAt(now);
        debt.setUpdatedAt(now);
        debts.save(debt);
        return invoice;
    }

    public List<Invoice> invoices(UUID partnerId, String status) {
        if (partnerId != null && status != null) {
            return invoices.findByPartnerIdAndStatusOrderByIssueDateDesc(partnerId, status);
        }
        if (partnerId != null) {
            return invoices.findByPartnerIdOrderByIssueDateDesc(partnerId);
        }
        return status != null ? invoices.findByStatusOrderByIssueDateDesc(status) : invoices.findAll();
    }

    public List<IssueableOrder> issueableOrders(UUID partnerId, UUID contractId) {
        if (partnerId == null) {
            throw new BusinessException("Invoice partner is required", HttpStatus.BAD_REQUEST);
        }
        UUID sourcePartnerA = partnerId;
        if (contractId != null) {
            Contract contract = contracts.findById(contractId)
                    .orElseThrow(() -> new BusinessException("Contract not found", HttpStatus.NOT_FOUND));
            LocalDate today = LocalDate.now(clock);
            if (!"ACTIVE".equals(contract.getStatus()) || today.isBefore(contract.getStartDate())
                    || today.isAfter(contract.getEndDate())
                    || (partnerId.equals(contract.getPartnerCId()) && !"A-C".equals(contract.getContractType()))
                    || (!partnerId.equals(contract.getPartnerAId()) && !partnerId.equals(contract.getPartnerCId()))) {
                throw new BusinessException("Select an active contract that includes this invoice partner",
                        HttpStatus.BAD_REQUEST);
            }
            sourcePartnerA = contract.getPartnerAId();
        }
        UUID sourceId = sourcePartnerA;
        return jdbc.query("""
                SELECT so.id, so.vehicle_id, v.plate_number, so.service_id, s.name,
                       so.total_amount, so.quantity, so.unit_price
                FROM service_orders so
                JOIN trips t ON t.id = so.trip_id
                JOIN pre_notifications pn ON pn.trip_id = t.id
                JOIN vehicles v ON v.id = so.vehicle_id
                JOIN services s ON s.id = so.service_id
                WHERE pn.partner_a_id = ?
                  AND so.status = 'COMPLETED'
                  AND pn.status IN ('CHECKED_IN', 'CHECKED_OUT')
                  AND EXISTS (
                      SELECT 1 FROM yard_logs yl
                      WHERE yl.pre_notification_id = pn.id AND yl.check_in_at IS NOT NULL
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM invoice_service_orders iso WHERE iso.service_order_id = so.id
                  )
                ORDER BY so.created_at DESC
                """, (resultSet, rowNumber) -> new IssueableOrder(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getObject("vehicle_id", UUID.class),
                    resultSet.getString("plate_number"),
                    resultSet.getObject("service_id", UUID.class),
                    resultSet.getString("name"),
                    resultSet.getBigDecimal("total_amount"),
                    resultSet.getBigDecimal("quantity"),
                    resultSet.getBigDecimal("unit_price")), sourceId);
    }

    public List<InvoiceLine> invoiceLines(UUID invoiceId) {
        invoice(invoiceId);
        return jdbc.query("""
                SELECT so.id AS service_order_id, v.plate_number, s.code AS service_code,
                       s.name AS service_name, so.quantity, so.unit_price, iso.line_amount
                FROM invoice_service_orders iso
                JOIN service_orders so ON so.id = iso.service_order_id
                JOIN vehicles v ON v.id = so.vehicle_id
                JOIN services s ON s.id = so.service_id
                WHERE iso.invoice_id = ?
                ORDER BY so.created_at, so.id
                """, (resultSet, rowNumber) -> new InvoiceLine(
                    resultSet.getObject("service_order_id", UUID.class),
                    resultSet.getString("plate_number"),
                    resultSet.getString("service_code"),
                    resultSet.getString("service_name"),
                    resultSet.getBigDecimal("quantity"),
                    resultSet.getBigDecimal("unit_price"),
                    resultSet.getBigDecimal("line_amount")), invoiceId);
    }

    public Invoice invoice(UUID invoiceId) {
        return invoices.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Invoice submitEInvoice(UUID invoiceId) {
        Invoice invoice = invoice(invoiceId);
        if ("SUBMITTED".equals(invoice.getEInvoiceStatus())) {
            throw new BusinessException("Invoice was already submitted to the e-invoice provider",
                    HttpStatus.CONFLICT);
        }
        invoice.setEInvoiceProviderId(eInvoiceGateway.submit(invoice));
        invoice.setEInvoiceStatus("SUBMITTED");
        invoice.setUpdatedAt(clock.instant());
        return invoices.save(invoice);
    }

    public java.nio.file.Path invoicePdf(UUID invoiceId) {
        Invoice invoice = invoice(invoiceId);
        if (invoice.getPdfUrl() == null) {
            throw new BusinessException("Invoice PDF has not been generated", HttpStatus.NOT_FOUND);
        }
        return invoiceDocuments.resolve(invoice.getPdfUrl());
    }

    @Transactional
    public Payment recordPayment(UUID invoiceId, BigDecimal amount, Instant paidAt, String referenceNo, String note,
            MultipartFile proof) {
        Invoice invoice = invoices.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found", HttpStatus.NOT_FOUND));
        if ("CANCELLED".equals(invoice.getStatus())) {
            throw new BusinessException("Cannot pay a cancelled invoice", HttpStatus.CONFLICT);
        }
        if (amount == null || amount.signum() <= 0 || paidAt == null) {
            throw new BusinessException("Positive payment amount and paid date are required", HttpStatus.BAD_REQUEST);
        }
        if ((referenceNo != null && referenceNo.length() > 100) || (note != null && note.length() > 500)) {
            throw new BusinessException("Payment reference or note exceeds its maximum length",
                    HttpStatus.BAD_REQUEST);
        }
        Debt debt = debts.findByInvoiceId(invoiceId).orElseThrow(
                () -> new BusinessException("Debt record missing for invoice", HttpStatus.INTERNAL_SERVER_ERROR));
        BigDecimal paymentAmount = amount.setScale(4, RoundingMode.HALF_UP);
        if (paymentAmount.signum() <= 0) {
            throw new BusinessException("Payment must be at least 0.0001", HttpStatus.BAD_REQUEST);
        }
        BigDecimal principalOutstanding = debt.getPrincipalAmount().subtract(debt.getPaidAmount()).max(BigDecimal.ZERO);
        BigDecimal interestOutstanding = debt.getInterestAccrued().subtract(debt.getInterestPaid()).max(BigDecimal.ZERO);
        if (paymentAmount.compareTo(principalOutstanding.add(interestOutstanding)) > 0) {
            throw new BusinessException("Payment cannot exceed the remaining principal and accrued interest",
                    HttpStatus.CONFLICT);
        }
        BigDecimal principalPayment = paymentAmount.min(principalOutstanding);
        BigDecimal interestPayment = paymentAmount.subtract(principalPayment);
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setInvoiceId(invoiceId);
        payment.setAmount(paymentAmount);
        payment.setPaidAt(paidAt);
        payment.setMethod("BANK_TRANSFER");
        payment.setReferenceNo(referenceNo);
        payment.setNote(note);
        payment.setCreatedAt(clock.instant());
        if (proof != null && !proof.isEmpty()) {
            validateProof(proof);
            payment.setProofUrl(storage.store(invoiceId, proof));
        }
        payments.save(payment);
        BigDecimal paid = debt.getPaidAmount().add(principalPayment);
        invoice.setStatus(paid.compareTo(invoice.getTotalAmount()) == 0 ? "PAID" :
                invoice.getDueDate().isBefore(LocalDate.now(clock)) ? "OVERDUE" : "UNPAID");
        invoice.setUpdatedAt(clock.instant());
        invoices.save(invoice);
        debt.setPaidAmount(paid.min(debt.getPrincipalAmount()));
        debt.setInterestPaid(debt.getInterestPaid().add(interestPayment).min(debt.getInterestAccrued()));
        debt.setStatus(debt.getPaidAmount().compareTo(debt.getPrincipalAmount()) >= 0
                && debt.getInterestPaid().compareTo(debt.getInterestAccrued()) >= 0 ? "PAID" : "OPEN");
        debt.setUpdatedAt(clock.instant());
        debts.save(debt);
        return payment;
    }

    private void validateProof(MultipartFile proof) {
        String type = proof.getContentType();
        if (proof.getSize() > 10L * 1024 * 1024 || type == null
                || !(type.equals("application/pdf") || type.equals("image/jpeg") || type.equals("image/png"))) {
            throw new BusinessException("Proof must be a PDF, JPEG or PNG file no larger than 10 MB",
                    HttpStatus.BAD_REQUEST);
        }
        try (var input = proof.getInputStream()) {
            byte[] header = input.readNBytes(8);
            boolean valid = switch (type) {
                case "application/pdf" -> header.length >= 5 && header[0] == '%'
                        && header[1] == 'P' && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
                case "image/jpeg" -> header.length >= 3 && (header[0] & 0xff) == 0xff
                        && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
                case "image/png" -> header.length == 8 && (header[0] & 0xff) == 0x89
                        && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                        && header[4] == 13 && header[5] == 10 && header[6] == 26 && header[7] == 10;
                default -> false;
            };
            if (!valid) {
                throw new BusinessException("Payment proof content does not match its file type",
                        HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new BusinessException("Unable to validate payment proof: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public DebtInterestConfig saveInterestConfig(UUID partnerId, BigDecimal ratePercent, int graceDays, boolean active) {
        if (ratePercent == null || ratePercent.signum() < 0 || graceDays < 0) {
            throw new BusinessException("Interest rate and grace days must not be negative", HttpStatus.BAD_REQUEST);
        }
        DebtInterestConfig config = interestConfigs.findByPartnerId(partnerId).orElseGet(() -> {
            DebtInterestConfig created = new DebtInterestConfig();
            created.setId(UUID.randomUUID());
            created.setPartnerId(partnerId);
            return created;
        });
        config.setRatePerDayPercent(ratePercent);
        config.setGraceDays(graceDays);
        config.setActive(active);
        config.setUpdatedAt(clock.instant());
        return interestConfigs.save(config);
    }

    public DebtInterestConfig interestConfig(UUID partnerId) {
        return interestConfigs.findByPartnerId(partnerId)
                .orElseThrow(() -> new BusinessException("Interest configuration not found",
                        HttpStatus.NOT_FOUND));
    }

    @Transactional
    public int markOverdue(LocalDate asOfDate) {
        List<Invoice> due = invoices.findByStatusAndDueDateBefore("UNPAID", asOfDate);
        due.forEach(invoice -> {
            invoice.setStatus("OVERDUE");
            invoice.setUpdatedAt(clock.instant());
        });
        invoices.saveAll(due);
        return due.size();
    }

    public List<Debt> debts(UUID partnerId, String status) {
        if (partnerId != null && status != null) {
            return debts.findByPartnerIdAndStatusOrderByDueDateAsc(partnerId, status);
        }
        if (partnerId != null) {
            return debts.findByPartnerIdOrderByDueDateAsc(partnerId);
        }
        return status != null ? debts.findByStatusOrderByDueDateAsc(status) : debts.findAll();
    }

    public List<Payment> payments(UUID invoiceId) {
        return payments.findByInvoiceIdOrderByPaidAtDesc(invoiceId);
    }

    public java.nio.file.Path paymentProof(UUID paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found", HttpStatus.NOT_FOUND));
        if (payment.getProofUrl() == null) {
            throw new BusinessException("Payment proof not found", HttpStatus.NOT_FOUND);
        }
        return storage.resolve(payment.getProofUrl());
    }

    public Reconciliation reconcile(UUID partnerAId, UUID partnerCId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Start date must not be after end date", HttpStatus.BAD_REQUEST);
        }
        PartnerLedger a = ledger(partnerAId, from, to);
        PartnerLedger c = ledger(partnerCId, from, to);
        return new Reconciliation(from, to, a, c,
                a.invoiced().subtract(c.invoiced()),
                a.paid().subtract(c.paid()),
                a.outstanding().subtract(c.outstanding()));
    }

    private PartnerLedger ledger(UUID partnerId, LocalDate from, LocalDate to) {
        List<Invoice> periodInvoices = invoices.findByPartnerIdAndIssueDateBetweenOrderByIssueDate(partnerId, from, to);
        List<Invoice> activeInvoices = periodInvoices.stream()
                .filter(invoice -> !"CANCELLED".equals(invoice.getStatus())).toList();
        BigDecimal invoiced = activeInvoices.stream().map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = activeInvoices.stream().map(invoice -> debts.findByInvoiceId(invoice.getId())
                .map(debt -> debt.getPrincipalAmount().subtract(debt.getPaidAmount())
                        .add(debt.getInterestAccrued()).subtract(debt.getInterestPaid()).max(BigDecimal.ZERO))
                .orElse(invoice.getTotalAmount().subtract(payments.sumForInvoice(invoice.getId()))
                        .max(BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = activeInvoices.stream().map(invoice -> payments.sumForInvoice(invoice.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PartnerLedger(partnerId, invoiced, paid, outstanding, periodInvoices.size());
    }

    public record PartnerLedger(UUID partnerId, BigDecimal invoiced, BigDecimal paid,
            BigDecimal outstanding, int invoiceCount) {
    }

    public record Reconciliation(LocalDate from, LocalDate to, PartnerLedger partnerA,
            PartnerLedger partnerC, BigDecimal differenceInvoiced, BigDecimal differencePaid,
            BigDecimal differenceOutstanding) {
    }

    public record IssueInvoiceRequest(UUID partnerId, UUID contractId, List<UUID> serviceOrderIds, LocalDate dueDate,
            BigDecimal taxRatePercent) {
    }

    public record IssueableOrder(UUID id, UUID vehicleId, String plateNumber, UUID serviceId,
            String serviceName, BigDecimal totalAmount, BigDecimal quantity, BigDecimal unitPrice) { }

    public record InvoiceLine(UUID serviceOrderId, String plateNumber, String serviceCode, String serviceName,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineAmount) { }
}
