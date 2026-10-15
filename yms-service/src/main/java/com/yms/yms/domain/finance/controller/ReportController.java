package com.yms.yms.domain.finance.controller;

import com.yms.common.api.ApiResponse;
import com.yms.yms.domain.finance.entity.ReportSchedule;
import com.yms.yms.domain.finance.service.ReportDeliveryService;
import com.yms.yms.domain.finance.service.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
public class ReportController {
    private final ReportService reports;
    private final ReportDeliveryService deliveryService;
    private final Clock clock;

    public ReportController(ReportService reports, ReportDeliveryService deliveryService, Clock clock) {
        this.reports = reports;
        this.deliveryService = deliveryService;
        this.clock = clock;
    }

    @GetMapping("/monthly")
    public ApiResponse<ReportService.MonthlyReport> monthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate today = LocalDate.now(clock);
        return ApiResponse.ok(reports.monthly(year == null ? today.getYear() : year,
                month == null ? today.getMonthValue() : month));
    }

    @GetMapping("/quarterly")
    public ApiResponse<ReportService.QuarterlyReport> quarterly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer quarter) {
        LocalDate today = LocalDate.now(clock);
        int selectedQuarter = quarter == null ? ((today.getMonthValue() - 1) / 3) + 1 : quarter;
        return ApiResponse.ok(reports.quarterly(year == null ? today.getYear() : year, selectedQuarter));
    }

    @GetMapping("/revenue-trend")
    public ApiResponse<java.util.List<ReportService.RevenuePoint>> revenueTrend(
            @RequestParam(required = false) Integer year) {
        LocalDate today = LocalDate.now(clock);
        return ApiResponse.ok(reports.revenueTrend(year == null ? today.getYear() : year));
    }

    @GetMapping("/dashboard")
    public ApiResponse<ReportService.Dashboard> dashboard() {
        return ApiResponse.ok(reports.dashboard());
    }

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<java.util.List<ReportSchedule>> schedules() {
        return ApiResponse.ok(deliveryService.listSchedules());
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<ReportSchedule> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(deliveryService.createSchedule(request.reportType(), request.recipientEmail()));
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ApiResponse<Void> deleteSchedule(@PathVariable java.util.UUID id) {
        deliveryService.deleteSchedule(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("yms-report.xlsx").build().toString())
                .body(reports.exportExcel(from, to));
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("yms-report.pdf").build().toString())
                .body(reports.exportPdf(from, to));
    }

    public record ScheduleRequest(@NotBlank String reportType, @NotBlank @Email String recipientEmail) {
    }
}
