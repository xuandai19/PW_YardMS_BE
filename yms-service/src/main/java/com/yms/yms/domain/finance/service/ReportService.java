package com.yms.yms.domain.finance.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.yms.common.exception.BusinessException;
import jakarta.transaction.Transactional;
import com.yms.yms.domain.finance.entity.Debt;
import com.yms.yms.domain.finance.entity.Invoice;
import com.yms.yms.domain.finance.repository.DebtRepository;
import com.yms.yms.domain.finance.repository.InvoiceRepository;
import com.yms.yms.domain.service.entity.ServiceOrder;
import com.yms.yms.domain.service.repository.ServiceOrderRepository;
import com.yms.yms.domain.yard.entity.Yard;
import com.yms.yms.domain.yard.entity.YardVehicleAssignment;
import com.yms.yms.domain.yard.repository.YardRepository;
import com.yms.yms.domain.yard.repository.YardVehicleAssignmentRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportService {
    private static final int EXPORT_ROW_LIMIT = 100_000;
    private final InvoiceRepository invoices;
    private final DebtRepository debts;
    private final ServiceOrderRepository serviceOrders;
    private final YardRepository yards;
    private final YardVehicleAssignmentRepository assignments;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public ReportService(InvoiceRepository invoices, DebtRepository debts, ServiceOrderRepository serviceOrders,
            YardRepository yards, YardVehicleAssignmentRepository assignments, JdbcTemplate jdbc, Clock clock) {
        this.invoices = invoices;
        this.debts = debts;
        this.serviceOrders = serviceOrders;
        this.yards = yards;
        this.assignments = assignments;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    public MonthlyReport monthly(int year, int month) {
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new BusinessException("Year or month is outside the supported range", HttpStatus.BAD_REQUEST);
        }
        YearMonth period = YearMonth.of(year, month);
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();
        OffsetDateTime sqlPeriodStart = from.atStartOfDay(clock.getZone()).toOffsetDateTime();
        OffsetDateTime sqlPeriodEnd = to.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime();
        List<Invoice> periodInvoices = invoices.findByIssueDateBetweenOrderByIssueDate(from, to);
        BigDecimal revenue = periodInvoices.stream().filter(i -> !"CANCELLED".equals(i.getStatus()))
                .map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long trips = jdbc.queryForObject("SELECT COUNT(*) FROM trips WHERE created_at >= ? AND created_at < ?",
                Long.class, sqlPeriodStart, sqlPeriodEnd);
        long vehicleEntries = jdbc.queryForObject(
                "SELECT COUNT(*) FROM yard_logs WHERE check_in_at >= ? AND check_in_at < ?",
                Long.class, sqlPeriodStart, sqlPeriodEnd);
        long capacity = yards.findAll().stream().filter(yard -> "ACTIVE".equals(yard.getStatus()))
                .mapToLong(Yard::getCapacity).sum();
        long occupied = yards.findAll().stream().filter(yard -> "ACTIVE".equals(yard.getStatus()))
                .mapToLong(yard -> assignments.countCurrentByYardId(yard.getId())).sum();
        Instant periodStart = from.atStartOfDay(clock.getZone()).toInstant();
        Instant periodEnd = to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        Instant measuredUntil = clock.instant().isBefore(periodEnd) ? clock.instant() : periodEnd;
        long occupiedVehicleSeconds = 0;
        if (measuredUntil.isAfter(periodStart)) {
            for (YardVehicleAssignment assignment : assignments.findOverlappingPeriod(periodStart, measuredUntil)) {
                Instant assignmentStart = assignment.getCheckInAt().isAfter(periodStart)
                        ? assignment.getCheckInAt() : periodStart;
                Instant checkout = assignment.getCheckOutAt() == null ? measuredUntil : assignment.getCheckOutAt();
                Instant assignmentEnd = checkout.isBefore(measuredUntil) ? checkout : measuredUntil;
                if (assignmentEnd.isAfter(assignmentStart)) {
                    occupiedVehicleSeconds += java.time.Duration.between(assignmentStart, assignmentEnd).getSeconds();
                }
            }
        }
        BigDecimal debtBalance = debts.findAll().stream()
                .map(debt -> debt.getPrincipalAmount().subtract(debt.getPaidAmount())
                        .add(debt.getInterestAccrued()).subtract(debt.getInterestPaid()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long measurementSeconds = Math.max(0, java.time.Duration.between(periodStart, measuredUntil).getSeconds());
        BigDecimal occupancyPercent = capacity == 0 || measurementSeconds == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(occupiedVehicleSeconds).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(capacity).multiply(BigDecimal.valueOf(measurementSeconds)),
                                2, RoundingMode.HALF_UP);
        return new MonthlyReport(year, month, revenue, periodInvoices.size(), trips, vehicleEntries, occupied,
                capacity, occupancyPercent, debtBalance);
    }

    public QuarterlyReport quarterly(int year, int quarter) {
        if (year < 1 || year > 9999 || quarter < 1 || quarter > 4) {
            throw new BusinessException("Year or quarter is outside the supported range", HttpStatus.BAD_REQUEST);
        }
        LocalDate from = LocalDate.of(year, ((quarter - 1) * 3) + 1, 1);
        LocalDate to = from.plusMonths(3).minusDays(1);
        List<Invoice> periodInvoices = invoices.findByIssueDateBetweenOrderByIssueDate(from, to).stream()
                .filter(i -> !"CANCELLED".equals(i.getStatus())).toList();
        BigDecimal revenue = periodInvoices.stream().map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<UUID, BigDecimal> partnerTotals = new LinkedHashMap<>();
        periodInvoices.forEach(invoice -> partnerTotals.merge(invoice.getPartnerId(), invoice.getTotalAmount(), BigDecimal::add));
        List<PartnerRevenue> topPartners = partnerTotals.entrySet().stream()
                .map(entry -> new PartnerRevenue(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PartnerRevenue::revenue).reversed()).limit(10).toList();
        List<ServiceOrder> periodOrders = serviceOrders.findAll().stream()
                .filter(order -> !order.getCreatedAt().isBefore(from.atStartOfDay(clock.getZone()).toInstant())
                        && order.getCreatedAt().isBefore(to.plusDays(1).atStartOfDay(clock.getZone()).toInstant()))
                .toList();
        long completed = periodOrders.stream().filter(order -> "COMPLETED".equals(order.getStatus())).count();
        BigDecimal serviceRevenue = periodOrders.stream().filter(order -> "COMPLETED".equals(order.getStatus()))
                .map(ServiceOrder::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new QuarterlyReport(year, quarter, revenue, periodInvoices.size(), topPartners,
                periodOrders.size(), completed, serviceRevenue);
    }

    public List<RevenuePoint> revenueTrend(int year) {
        if (year < 1 || year > 9999) {
            throw new BusinessException("Year is outside the supported range", HttpStatus.BAD_REQUEST);
        }
        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    YearMonth period = YearMonth.of(year, month);
                    BigDecimal revenue = invoices.findByIssueDateBetweenOrderByIssueDate(
                                    period.atDay(1), period.atEndOfMonth()).stream()
                            .filter(invoice -> !"CANCELLED".equals(invoice.getStatus()))
                            .map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new RevenuePoint(period.toString(), revenue);
                }).toList();
    }

    public Dashboard dashboard() {
        long vehiclesInYard = yards.findAll().stream()
                .mapToLong(yard -> assignments.countCurrentByYardId(yard.getId())).sum();
        long vehiclesInTrip = jdbc.queryForObject("SELECT COUNT(*) FROM trips WHERE status = 'IN_PROGRESS'",
                Long.class);
        long activeAlerts = jdbc.queryForObject("SELECT COUNT(*) FROM yard_alerts WHERE resolved_at IS NULL",
                Long.class);
        BigDecimal outstanding = debts.findAll().stream()
                .map(debt -> debt.getPrincipalAmount().subtract(debt.getPaidAmount())
                        .add(debt.getInterestAccrued()).subtract(debt.getInterestPaid()).max(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dashboard(vehiclesInYard, vehiclesInTrip, activeAlerts, outstanding, clock.instant());
    }

    @Transactional
    public byte[] exportExcel(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        List<Invoice> rows = invoices.findByIssueDateBetweenOrderByIssueDate(from, to);
        if (rows.size() > EXPORT_ROW_LIMIT) {
            throw new BusinessException("Export is limited to 100,000 invoices per request", HttpStatus.BAD_REQUEST);
        }
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Invoices");
            Row header = sheet.createRow(0);
            String[] columns = {"Invoice No", "Partner ID", "Issue Date", "Due Date", "Subtotal", "Tax", "Total", "Status"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowIndex = 1;
            for (Invoice invoice : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(invoice.getInvoiceNo());
                row.createCell(1).setCellValue(invoice.getPartnerId().toString());
                row.createCell(2).setCellValue(invoice.getIssueDate().toString());
                row.createCell(3).setCellValue(invoice.getDueDate().toString());
                row.createCell(4).setCellValue(invoice.getSubtotal().toPlainString());
                row.createCell(5).setCellValue(invoice.getTaxAmount().toPlainString());
                row.createCell(6).setCellValue(invoice.getTotalAmount().toPlainString());
                row.createCell(7).setCellValue(invoice.getStatus());
            }
            workbook.write(output);
            workbook.dispose();
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Unable to export Excel report: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] exportPdf(LocalDate from, LocalDate to) {
        validatePeriod(from, to);
        List<Invoice> rows = invoices.findByIssueDateBetweenOrderByIssueDate(from, to);
        if (rows.size() > EXPORT_ROW_LIMIT) {
            throw new BusinessException("Export is limited to 100,000 invoices per request", HttpStatus.BAD_REQUEST);
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (Document document = new Document(new PdfDocument(new PdfWriter(output)))) {
                document.add(new Paragraph("YMS INVOICE REPORT"));
                document.add(new Paragraph("Period: " + from + " to " + to));
                document.add(new Paragraph("Invoice count: " + rows.size()));
                BigDecimal total = rows.stream().filter(invoice -> !"CANCELLED".equals(invoice.getStatus()))
                        .map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                document.add(new Paragraph("Revenue: " + total.toPlainString()));
                for (Invoice invoice : rows) {
                    document.add(new Paragraph(invoice.getInvoiceNo() + " | " + invoice.getPartnerId()
                            + " | " + invoice.getIssueDate() + " | " + invoice.getTotalAmount()
                            + " | " + invoice.getStatus()));
                }
            }
            return output.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("Unable to export PDF report: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validatePeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Start date must not be after end date", HttpStatus.BAD_REQUEST);
        }
    }

    public record MonthlyReport(int year, int month, BigDecimal revenue, int invoiceCount,
            long trips, long vehicleEntries, long vehiclesCurrentlyInYards, long yardCapacity,
            BigDecimal occupancyPercent, BigDecimal outstandingDebt) {
    }

    public record QuarterlyReport(int year, int quarter, BigDecimal revenue, int invoiceCount,
            List<PartnerRevenue> topPartners, int serviceOrderCount, long completedServiceOrders,
            BigDecimal completedServiceRevenue) {
    }

    public record PartnerRevenue(UUID partnerId, BigDecimal revenue) {
    }

    public record RevenuePoint(String month, BigDecimal revenue) {
    }

    public record Dashboard(long vehiclesInYard, long vehiclesInTrip, long activeAlerts,
            BigDecimal outstandingDebt, Instant generatedAt) {
    }
}
